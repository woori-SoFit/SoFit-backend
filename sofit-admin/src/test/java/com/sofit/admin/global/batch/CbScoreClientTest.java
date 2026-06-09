package com.sofit.admin.global.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CbScoreClient 단위 테스트")
class CbScoreClientTest {

    @Test
    @DisplayName("External Mock 서버 연결 불가 시 재시도 후 null을 반환한다")
    void getCbScore_서버_연결_불가시_재시도_후_null_반환() {
        // given — 존재하지 않는 주소로 생성하여 연결 실패 유도
        CbScoreClient cbScoreClient = new CbScoreClient("http://localhost:1");

        // when
        Integer score = cbScoreClient.getCbScore("홍길동", "9001011");

        // then — 3회 재시도 후 null 반환
        assertThat(score).isNull();
    }

    @Test
    @DisplayName("스레드 인터럽트 발생 시 즉시 null을 반환한다")
    void getCbScore_인터럽트_발생시_null_반환() {
        // given — 연결 실패하는 클라이언트 + 스레드 인터럽트 설정
        CbScoreClient cbScoreClient = new CbScoreClient("http://localhost:1");

        // when — 현재 스레드에 인터럽트 플래그 설정
        Thread.currentThread().interrupt();
        Integer score = cbScoreClient.getCbScore("테스트", "0000001");

        // then — 인터럽트로 인해 재시도 중단 후 null 반환
        assertThat(score).isNull();
        // 인터럽트 상태 정리
        Thread.interrupted();
    }
}
