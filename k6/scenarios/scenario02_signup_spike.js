/**
 * 시나리오 02 — 회원가입 동시 폭주 + FastAPI 단건 S등급 산출
 *
 * 목적: 100 VU 동시 가입 시 S-Grade 비동기 풀(core=10/max=20/queue=150) 고갈 여부 확인
 * 부하: 0→100 VU (15초) → 100 VU 유지 2분
 *
 * 합격 기준:
 *   - 회원가입 완료 p(95)     < 3,000ms
 *   - 회원가입 자체 실패율    < 1%
 *   - FastAPI 오류율          < 5%  ← 테스트 종료 후 DB 확인 필요 (아래 쿼리 참고)
 *
 * ⚠️  FastAPI(@Async) 주의사항:
 *   completeSignup HTTP 응답은 FastAPI 완료를 기다리지 않는다.
 *   k6로는 FastAPI 성공/실패를 직접 측정할 수 없다.
 *   테스트 종료 후 아래 쿼리로 FAILED 건수를 확인한다.
 *
 *   SELECT status, COUNT(*) FROM s_grade_history
 *   WHERE requested_at >= NOW() - INTERVAL 10 MINUTE
 *   GROUP BY status;
 *
 *   COMPLETED: FastAPI 성공
 *   FAILED:    FastAPI 실패 or sGradeExecutor 풀 초과 거절
 *   REQUESTED: FastAPI 아직 처리 중 (테스트 직후 조회 시 일부 해당)
 *
 * ⚠️  사전 조건:
 *   - 아래 테스트 데이터가 실제 외부 인증(공공API)을 통과하거나
 *     mock 환경에서 실행해야 한다. (business-verification, verify-pin 단계)
 *   - 동일한 loginId/businessNumber/phoneNumber로 재실행 시 중복 오류 발생.
 *     재실행 전 테스트 계정을 DB에서 삭제하거나 VU_OFFSET 환경 변수로 범위를 밀어야 한다.
 *
 * 실행:
 *   k6 run k6/scenario02_signup_spike.js
 *   k6 run --env BASE_URL=http://xn--ip-v41jw5m:8080 --env VU_OFFSET=0 k6/scenario02_signup_spike.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL  = __ENV.BASE_URL  || 'http://localhost:8080';
const VU_OFFSET = parseInt(__ENV.VU_OFFSET || '0'); // 재실행 시 계정 범위 이동용

// ── 단계별 응답 시간 ──────────────────────────────────────────────
const bizVerifyDuration    = new Trend('biz_verify_duration',    true);
const pinVerifyDuration    = new Trend('pin_verify_duration',    true);
const signupCompleteDuration = new Trend('signup_complete_duration', true);

// ── 단계별 에러율 ─────────────────────────────────────────────────
const bizVerifyErrorRate      = new Rate('biz_verify_error_rate');
const pinVerifyErrorRate      = new Rate('pin_verify_error_rate');
const signupCompleteErrorRate = new Rate('signup_complete_error_rate');

// 플로우 중단 카운터 (앞 단계 실패로 complete까지 못 간 건)
const flowAbortCount = new Counter('flow_abort_count');

// ── 테스트 계정 데이터 ────────────────────────────────────────────
// business-verification과 verify-pin은 외부 API(공공 데이터 포털, NICE 등)를 타거나
// mock 처리되므로 실제 환경에 맞는 테스트 값으로 교체해야 한다.
const users = new SharedArray('users', () =>
    JSON.parse(open('./users.json')));

// ── 부하 설정 ────────────────────────────────────────────────────
export const options = {
    scenarios: {
        signup_spike: {
            executor:     'per-vu-iterations',
            vus:          100,
            iterations:   1,    // 각 VU가 정확히 1번씩 = 총 100회
            maxDuration:  '3m',
        },
    },
    thresholds: {
        signup_complete_duration: ['p(95)<3000'],
        signup_complete_error_rate: ['rate<0.01'],
        // biz/pin 단계는 합격 기준 외 참고용 측정
        biz_verify_error_rate: ['rate<0.05'],
        pin_verify_error_rate: ['rate<0.05'],
    },
};

// ── setup: 약관 ID 목록 조회 ─────────────────────────────────────
export function setup() {
    const res = http.get(`${BASE_URL}/api/terms?termType=PERSONAL_INFO`);
    console.log(`[setup] 약관 조회 status=${res.status}`);
    console.log(`[setup] 약관 조회 body=${res.body}`);

    if (res.status !== 200) {
        console.error(`약관 조회 실패: status=${res.status}`);
        return { consents: [] };
    }

    let parsed;
    try {
        parsed = JSON.parse(res.body);
    } catch (e) {
        console.error(`[setup] JSON 파싱 실패: ${e}`);
        return { consents: [] };
    }

    // result가 배열인지, 아니면 다른 구조인지 대응
    let terms = [];
    if (Array.isArray(parsed.result)) {
        terms = parsed.result;
    } else if (parsed.result && Array.isArray(parsed.result.terms)) {
        terms = parsed.result.terms;
    } else if (Array.isArray(parsed)) {
        terms = parsed;
    } else {
        console.error(`[setup] 예상치 못한 응답 구조: ${JSON.stringify(parsed.result)}`);
        return { consents: [] };
    }

    const consents = terms.map(t => ({
        termId:      t.termId || t.id,
        isConsented: true,
    }));

    console.log(`약관 ${consents.length}건 로드 완료`);
    return { consents };
}

// ── 메인 플로우 ───────────────────────────────────────────────────
export default function (data) {
    const { consents } = data;

    // VU_OFFSET으로 재실행 시 계정 범위 이동 (중복 가입 방지)
    const user = users[(__VU - 1 + VU_OFFSET) % users.length];

    // k6는 VU별 cookieJar를 자동 관리 → 세션 쿠키가 단계 간 자동 전달됨
    const jar = http.cookieJar();
    jar.clear(BASE_URL);

    // ── 1단계: 사업자 인증 ────────────────────────────────────────
    const bizRes = http.post(
        `${BASE_URL}/api/auth/signup/business-verification`,
        JSON.stringify({ businessNumber: user.businessNumber }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags:    { name: 'biz_verify' },
        }
    );

    const bizOk = check(bizRes, {
        'biz_verify: status 200':            (r) => r.status === 200,
        'biz_verify: representativeName 있음': (r) => {
            try { return JSON.parse(r.body)?.result?.representativeName != null; } catch { return false; }
        },
    });

    bizVerifyDuration.add(bizRes.timings.duration);
    bizVerifyErrorRate.add(!bizOk);

    if (!bizOk) {
        flowAbortCount.add(1);
        sleep(1);
        return;
    }

    sleep(0.5);

    // ── 2단계: 금융인증서 PIN 검증 ────────────────────────────────
    const pinRes = http.post(
        `${BASE_URL}/api/auth/signup/verify-pin`,
        JSON.stringify({
            phoneNumber:    user.phone,
            holderName:     user.holderName,
            residentNumber: user.residentNumber,
            pin:            user.pin,
        }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags:    { name: 'pin_verify' },
        }
    );

    const pinOk = check(pinRes, {
        'pin_verify: status 200': (r) => r.status === 200,
    });

    pinVerifyDuration.add(pinRes.timings.duration);
    pinVerifyErrorRate.add(!pinOk);

    if (!pinOk) {
        flowAbortCount.add(1);
        sleep(1);
        return;
    }

    sleep(0.5);

    // ── 3단계: 가입 완료 (→ 내부에서 @Async FastAPI S등급 요청 발생) ──
    const completeRes = http.post(
        `${BASE_URL}/api/auth/signup/complete`,
        JSON.stringify({
            loginId:        `sofitUser${String(user.idx).padStart(3, '0')}`,
            password:       'Test1234!',
            name:           user.holderName,
            residentNumber: user.residentNumber,
            phoneNumber:    user.phone,
            consents,
        }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags:    { name: 'signup_complete' },
        }
    );

    const completeOk = check(completeRes, {
        'signup_complete: status 201':  (r) => r.status === 201,
        'signup_complete: userId 있음': (r) => {
            try { return JSON.parse(r.body)?.result?.userId != null; } catch { return false; }
        },
    });

    // 디버깅: 실패 시 응답 확인
    if (!completeOk) {
        console.log(`[signup_complete] status=${completeRes.status} body=${completeRes.body}`);
    }

    signupCompleteDuration.add(completeRes.timings.duration);
    signupCompleteErrorRate.add(!completeOk);

    // predictAsync는 @Async("sGradeExecutor") 백그라운드 실행
    // AsyncConfig: core=10, max=20, queue=150 → 100 VU 동시 가입 수용 가능
    // 성공/실패 여부는 테스트 종료 후 s_grade_history 테이블로 확인

    sleep(1);
}

// ── teardown: FastAPI 결과 확인 안내 ─────────────────────────────
export function teardown() {
    console.log('');
    console.log('=== FastAPI S등급 결과 확인 (테스트 종료 30초 후 실행) ===');
    console.log('SELECT status, COUNT(*) as cnt');
    console.log('FROM s_grade_history');
    console.log("WHERE requested_at >= NOW() - INTERVAL 10 MINUTE");
    console.log('GROUP BY status;');
    console.log('');
    console.log('COMPLETED 비율 = FastAPI 정상 처리');
    console.log('FAILED 비율    = FastAPI 오류 또는 sGradeExecutor 풀 포화 거절');
    console.log('REQUESTED 잔존 = 아직 처리 중 (비동기 지연)');
}