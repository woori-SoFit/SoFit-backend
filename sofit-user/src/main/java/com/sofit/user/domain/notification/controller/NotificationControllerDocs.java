package com.sofit.user.domain.notification.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.user.domain.notification.dto.response.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림", description = "알림 SSE 구독, 미읽음 조회, 읽음 처리 API")
public interface NotificationControllerDocs {

    @Operation(summary = "SSE 구독", description = "실시간 알림 수신을 위한 SSE 연결을 수립합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 연결 수립 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다 (COMMON4001)")
    })
    SseEmitter subscribe();

    @Operation(summary = "미읽음 알림 조회", description = "미읽음 알림 목록을 조회합니다. (최대 100건, 최신순)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (NOTI2000)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다 (COMMON4001)")
    })
    ApiResponse<NotificationListResponse> getUnread();

    @Operation(summary = "전체 알림 목록 조회", description = "읽음/미읽음 모두 포함한 전체 알림 목록을 조회합니다. (최신순)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (NOTI2003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다 (COMMON4001)")
    })
    ApiResponse<NotificationListResponse> getAll();

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공 (NOTI2001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 알림에 대한 권한이 없습니다 (NOTI4003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 알림을 찾을 수 없습니다 (NOTI4004)")
    })
    ApiResponse<Void> markAsRead(
            @Parameter(description = "알림 ID", required = true, example = "1") Long notificationId
    );

    @Operation(summary = "내부 알림 푸시", description = "sofit-admin에서 호출하는 내부 SSE 푸시 API입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "푸시 성공 (NOTI2002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다 (COMMON4000)")
    })
    ApiResponse<Void> push(NotificationPushRequest request);
}
