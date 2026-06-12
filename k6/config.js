// 공통 설정 및 헬퍼 함수
export const BASE_URL_USER  = __ENV.BASE_URL_USER  || 'http://localhost:8080';
export const BASE_URL_ADMIN = __ENV.BASE_URL_ADMIN || 'http://localhost:8081';

export const THRESHOLDS = {
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
  http_req_failed:   ['rate<0.01'],
};

// 세션 쿠키를 헤더로 변환하는 헬퍼
export function cookieHeader(jar, url) {
  // k6 CookieJar는 자동으로 쿠키를 전송하므로 별도 처리 불필요
  // 이 함수는 명시적 쿠키 전달이 필요한 경우 사용
  return {};
}

// 공통 JSON 헤더
export const JSON_HEADERS = {
  'Content-Type': 'application/json',
};

// 임의 6자리 숫자 문자열
export function randomDigits(n = 6) {
  return Array.from({ length: n }, () => Math.floor(Math.random() * 10)).join('');
}

// 고유 로그인 ID 생성
export function uniqueLoginId(prefix = 'user') {
  return `${prefix}_${Date.now()}_${randomDigits(4)}`;
}

// 테스트용 사업자 번호 (고정 값 — 실제 환경에 맞게 교체)
export const TEST_USER_CREDENTIALS = [
  { loginId: 'testuser01', password: 'Test1234!' },
  { loginId: 'testuser02', password: 'Test1234!' },
  { loginId: 'testuser03', password: 'Test1234!' },
];

export const TEST_ADMIN_TELLER = { loginId: 'teller01', password: 'Admin1234!' };
export const TEST_ADMIN_MANAGER = { loginId: 'manager01', password: 'Admin1234!' };

// VU 인덱스에 따라 테스트 계정 순환 선택
export function pickUser(vu) {
  return TEST_USER_CREDENTIALS[vu % TEST_USER_CREDENTIALS.length];
}

// 응답 체크 헬퍼
export function check2xx(res, label) {
  return res.status >= 200 && res.status < 300;
}
