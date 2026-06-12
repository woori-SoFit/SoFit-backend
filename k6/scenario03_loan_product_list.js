/**
 * 시나리오 03 — 대출 상품 목록 조회 (TPS 베이스라인)
 *
 * 목적: 비인증 공개 API의 TPS 베이스라인 확보 및 DB 읽기 성능 확인
 * 부하: 50 VU 일정 부하 3분
 * 합격 기준: 상품 목록/상세 p(95) < 300ms
 *
 * 실행: k6 run k6/scenario03_loan_product_list.js
 * 환경 변수: BASE_URL (기본값: http://localhost:8080)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 엔드포인트별 응답 시간 분리 측정
const productListDuration   = new Trend('product_list_duration',   true);
const productDetailDuration = new Trend('product_detail_duration', true);

// 엔드포인트별 에러율
const productListErrorRate   = new Rate('product_list_error_rate');
const productDetailErrorRate = new Rate('product_detail_error_rate');

export const options = {
    scenarios: {
        loan_product_baseline: {
            executor: 'constant-vus',
            vus: 50,
            duration: '3m',
        },
    },
    thresholds: {
        // 합격 기준
        product_list_duration:   ['p(95)<300'],
        product_detail_duration: ['p(95)<300'],
        // 에러율 보조 기준
        product_list_error_rate:   ['rate<0.01'],
        product_detail_error_rate: ['rate<0.01'],
    },
};

export default function () {
    // 1단계: 상품 목록 조회
    const listRes = http.get(`${BASE_URL}/api/loan-products`, {
        tags: { name: 'product_list' },
    });

    const listOk = check(listRes, {
        'product_list: status 200':          (r) => r.status === 200,
        'product_list: body is array':       (r) => {
            try { return Array.isArray(JSON.parse(r.body)?.result?.loanProducts); } catch { return false; }
        },
    });

    productListDuration.add(listRes.timings.duration);
    productListErrorRate.add(!listOk);

    if (!listOk) {
        sleep(1);
        return;
    }

    // 목록 응답에서 productId 추출 (없으면 스킵)
    let products;
    try {
        products = JSON.parse(listRes.body)?.result?.loanProducts;
    } catch {
        sleep(1);
        return;
    }

    if (!products || products.length === 0) {
        sleep(1);
        return;
    }

    // VU마다 다른 상품을 조회해 DB 캐시 편향 방지
    const product = products[__VU % products.length];
    const productId = product.productId ?? product.id;

    sleep(0.5); // 목록 → 상세 사이 사용자 클릭 딜레이

    // 2단계: 상품 상세 조회
    const detailRes = http.get(`${BASE_URL}/api/loan-products/${productId}`, {
        tags: { name: 'product_detail' },
    });

    const detailOk = check(detailRes, {
        'product_detail: status 200':     (r) => r.status === 200,
        'product_detail: has productId':  (r) => {
            try { return JSON.parse(r.body)?.result?.productId != null; } catch { return false; }
        },
    });

    productDetailDuration.add(detailRes.timings.duration);
    productDetailErrorRate.add(!detailOk);

    sleep(1); // 다음 반복 전 대기
}
