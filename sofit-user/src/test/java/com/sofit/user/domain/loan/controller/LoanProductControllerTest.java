package com.sofit.user.domain.loan.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductOptionsResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.service.LoanProductService;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(LoanProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LoanProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanProductService loanProductService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 1L;

    // === GET /api/loan-products ===

    @Test
    @DisplayName("대출 상품 목록 조회 - 200 + LOAN2001 반환")
    void getProducts_returns200() throws Exception {
        // given
        LoanProductListResponse response = LoanProductListResponse.builder()
                .loanProducts(List.of(
                        LoanProductListResponse.LoanProductItem.builder()
                                .productId(1L)
                                .productName("소상공인 성장 대출")
                                .title("성장을 위한 대출")
                                .maxLimit(50_000_000L)
                                .build()
                ))
                .build();

        given(loanProductService.findProducts()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-products")
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2001"))
                .andExpect(jsonPath("$.result.loanProducts").isArray())
                .andExpect(jsonPath("$.result.loanProducts[0].productId").value(1))
                .andExpect(jsonPath("$.result.loanProducts[0].productName").value("소상공인 성장 대출"));
    }

    @Test
    @DisplayName("대출 상품 목록 조회 - ACTIVE 상품이 없으면 빈 목록 반환")
    void getProducts_returnsEmptyList_whenNoActiveProducts() throws Exception {
        // given
        given(loanProductService.findProducts())
                .willReturn(LoanProductListResponse.builder().loanProducts(List.of()).build());

        // when & then
        mockMvc.perform(get("/api/loan-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2001"))
                .andExpect(jsonPath("$.result.loanProducts").isEmpty());
    }

    // === GET /api/loan-products/{productId} ===

    @Test
    @DisplayName("대출 상품 상세 조회 - 200 + LOAN2002 반환")
    void getProduct_returns200() throws Exception {
        // given
        LoanProductDetailResponse response = LoanProductDetailResponse.builder()
                .productId(PRODUCT_ID)
                .productName("소상공인 성장 대출")
                .title("성장을 위한 대출")
                .subtitle("소상공인 전용")
                .minLimit(1_000_000L)
                .maxLimit(50_000_000L)
                .maxTerm(60)
                .targetSummary("소상공인 대상")
                .filterConditions(LoanProductDetailResponse.FilterConditions.builder()
                        .annualIncomeLimit(new BigDecimal("50000000"))
                        .creditScoreLimit((short) 600)
                        .build())
                .interestRate(LoanProductDetailResponse.InterestRate.builder()
                        .minRate(new BigDecimal("3.50"))
                        .maxRate(new BigDecimal("8.00"))
                        .build())
                .build();

        given(loanProductService.findProduct(PRODUCT_ID)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-products/{productId}", PRODUCT_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2002"))
                .andExpect(jsonPath("$.result.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.result.productName").value("소상공인 성장 대출"));
    }

    @Test
    @DisplayName("대출 상품 상세 조회 - 존재하지 않는 상품이면 404 반환")
    void getProduct_returns404_whenNotFound() throws Exception {
        // given
        given(loanProductService.findProduct(PRODUCT_ID))
                .willThrow(new BaseException(LoanErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/loan-products/{productId}", PRODUCT_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4041"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 대출 상품입니다."));
    }

    // === GET /api/loan-products/{productId}/options ===

    @Test
    @DisplayName("대출 상품 옵션 조회 - 200 + LOAN2012 반환")
    void getProductOptions_returns200() throws Exception {
        // given
        LoanProductOptionsResponse response = LoanProductOptionsResponse.builder()
                .productId(PRODUCT_ID)
                .productName("소상공인 성장 대출")
                .minLimit(1_000_000L)
                .maxLimit(50_000_000L)
                .loanOptions(List.of(
                        LoanProductOptionsResponse.LoanOptionItem.builder()
                                .purpose(LoanPurpose.WORKING_CAPITAL)
                                .repaymentMethod(RepaymentMethod.EQUAL_PAYMENT)
                                .maxTermMonths(36)
                                .build()
                ))
                .build();

        given(loanProductService.findProductOptions(PRODUCT_ID)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-products/{productId}/options", PRODUCT_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2012"))
                .andExpect(jsonPath("$.result.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.result.loanOptions").isArray())
                .andExpect(jsonPath("$.result.loanOptions[0].purpose").value("WORKING_CAPITAL"));
    }

    @Test
    @DisplayName("대출 상품 옵션 조회 - 존재하지 않는 상품이면 404 반환")
    void getProductOptions_returns404_whenNotFound() throws Exception {
        // given
        given(loanProductService.findProductOptions(PRODUCT_ID))
                .willThrow(new BaseException(LoanErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/loan-products/{productId}/options", PRODUCT_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4041"));
    }
}
