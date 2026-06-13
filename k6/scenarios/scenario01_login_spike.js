/**
 * 시나리오 01 — 사용자 로그인 스파이크
 *
 * 목적: 동시 100 VU 로그인이 Redis 세션 저장소 / BCrypt CPU에 미치는 영향 측정
 * 부하: 0→100 VU (10초) → 100 VU 유지 2분 → 0 VU
 * 합격 기준:
 *   - 로그인 p(95)     < 1,500ms
 *   - 로그인 성공률    > 99%
 *   - 프로필 조회 p(95) < 500ms
 *
 * 사전 조건: seed 계정 100개 이상 (loginId: sofitUser000~099, password: Test1234!)
 * 실행: k6 run k6/scenario01_login_spike.js
 * 환경 변수:
 *   BASE_URL   (기본값: http://localhost:8080)
 *   USER_COUNT (기본값: 100, seed 계정 수와 맞출 것)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const USER_COUNT = parseInt(__ENV.USER_COUNT || '100');

// SharedArray: 모든 VU가 공유, 파싱은 init 단계에서 1회만 수행
const users = new SharedArray('users', function () {
    const list = [];
    for (let i = 0; i < USER_COUNT; i++) {
        list.push({
            loginId:  `sofitUser${String(i).padStart(3, '0')}`,
            password: 'Test1234!',
        });
    }
    return list;
});

// 엔드포인트별 응답 시간
const loginDuration   = new Trend('login_duration',   true);
const profileDuration = new Trend('profile_duration', true);

// 로그인 성공/실패
const loginErrorRate   = new Rate('login_error_rate');
const profileErrorRate = new Rate('profile_error_rate');

// 세션 발급 실패 절대 카운터
const sessionFailCount = new Counter('session_fail_count');

export const options = {
    scenarios: {
        login_spike: {
            executor:    'ramping-vus',
            startVUs:    0,
            stages: [
                { duration: '10s', target: 100 }, // 스파이크
                { duration: '2m',  target: 100 }, // 유지
                { duration: '5s',  target: 0   }, // 종료
            ],
        },
    },
    thresholds: {
        login_duration:   ['p(95)<1500'],
        profile_duration: ['p(95)<500'],
        login_error_rate: ['rate<0.01'],  // 로그인 실패율 < 1%
    },
};

export default function () {
    // VU마다 다른 계정 사용 (동일 계정 동시 로그인으로 인한 세션 충돌 방지)
    const user = users[__VU % users.length];

    // k6는 VU별로 cookieJar를 자동 관리하므로 로그인 후 세션 쿠키가 자동으로 유지됨
    const jar = http.cookieJar();
    jar.clear(BASE_URL);

    // 1단계: 로그인
    const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ loginId: user.loginId, password: user.password }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags:    { name: 'login' },
        }
    );

    const loginOk = check(loginRes, {
        'login: status 200':      (r) => r.status === 200,
        'login: userId returned': (r) => {
            try { return JSON.parse(r.body)?.result?.userId != null; } catch { return false; }
        },
    });

    loginDuration.add(loginRes.timings.duration);
    loginErrorRate.add(!loginOk);

    if (!loginOk) {
        sessionFailCount.add(1);
        sleep(1);
        return;
    }

    sleep(0.3); // 로그인 직후 앱 초기화 딜레이

    // 2단계: 세션 정상 발급 확인 (프로필 조회)
    const profileRes = http.get(`${BASE_URL}/api/users/me`, {
        tags: { name: 'profile' },
    });

    const profileOk = check(profileRes, {
        'profile: status 200':    (r) => r.status === 200,
        'profile: not 401':       (r) => r.status !== 401,
        'profile: userId exists': (r) => {
            try { return JSON.parse(r.body)?.result?.userId != null; } catch { return false; }
        },
    });

    profileDuration.add(profileRes.timings.duration);
    profileErrorRate.add(!profileOk);

    if (!profileOk) {
        sessionFailCount.add(1);
    }

    sleep(1);
}