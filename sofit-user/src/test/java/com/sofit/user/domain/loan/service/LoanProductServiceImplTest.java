package com.sofit.user.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanProductOption;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.ProductStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.repository.loan.LoanProductOptionRepository;
import com.sofit.common.repository.loan.LoanProductRepository;
import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductOptionsResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceImplTest {

    @InjectMocks
    private LoanProductServiceImpl loanProductService;

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private LoanProductOptionRepository loanProductOptionRepository;

    private static final Long PRODUCT_ID = 1L;

    // --- findProducts ---

    @Test
    @DisplayName("findProducts - ACTIVE 상품 목록을 정상 반환한다")
    void findProducts_returnsActiveProducts() {
        // given
        LoanProduct product1 = createProduct(1L, "소상공인 성장 대출", "사업 성장을 위한 대출", 50_000_000L);
        LoanProduct product2 = createProduct(2L, "소상공인 운영 대출", "운영 자금 대출", 30_000_000L);

        given(loanProductRepository.findByStatus(ProductStatus.ACTIVE))
                .willReturn(List.of(product1, product2));

        // when
        LoanProductListResponse response = loanProductService.findProducts();

        // then
        assertThat(response.getLoanProducts()).hasSize(2);
        assertThat(response.getLoanProducts().get(0).getProductName()).isEqualTo("소상공인 성장 대출");
        assertThat(response.getLoanProducts().get(1).getProductName()).isEqualTo("소상공인 운영 대출");
    }

    @Test
    @DisplayName("findProducts - ACTIVE 상품이 없으면 빈 목록을 반환한다")
    void findProducts_returnsEmptyList_whenNoActiveProducts() {
        // given
        given(loanProductRepository.findByStatus(ProductStatus.ACTIVE))
                .willReturn(List.of());

        // when
        LoanProductListResponse response = loanProductService.findProducts();

        // then
        assertThat(response.getLoanProducts()).isEmpty();
    }

    // --- findProduct ---

    @Test
    @DisplayName("findProduct - 상품 상세 정보를 정상 반환한다")
    void findProduct_returnsProductDetail() {
        // given
        LoanProduct product = createProduct(PRODUCT_ID, "소상공인 성장 대출", "사업 성장을 위한 대출", 50_000_000L);

        given(loanProductRepository.findById(PRODUCT_ID))
                .willReturn(Optional.of(product));

        // when
        LoanProductDetailResponse response = loanProductService.findProduct(PRODUCT_ID);

        // then
        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getProductName()).isEqualTo("소상공인 성장 대출");
        assertThat(response.getMaxLimit()).isEqualTo(50_000_000L);
    }

    @Test
    @DisplayName("findProduct - 존재하지 않는 상품이면 PRODUCT_NOT_FOUND 예외")
    void findProduct_throwsException_whenProductNotFound() {
        // given
        given(loanProductRepository.findById(PRODUCT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanProductService.findProduct(PRODUCT_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(LoanErrorCode.PRODUCT_NOT_FOUND);
                });
    }

    // --- findProductOptions ---

    @Test
    @DisplayName("findProductOptions - 상품 옵션 목록을 정상 반환한다")
    void findProductOptions_returnsOptions() {
        // given
        LoanProduct product = createProduct(PRODUCT_ID, "소상공인 성장 대출", "사업 성장을 위한 대출", 50_000_000L);
        LoanProductOption option1 = createOption(1L, LoanPurpose.WORKING_CAPITAL, RepaymentMethod.EQUAL_PAYMENT, 36);
        LoanProductOption option2 = createOption(2L, LoanPurpose.FACILITY_CAPITAL, RepaymentMethod.EQUAL_PRINCIPAL, 60);

        given(loanProductRepository.findById(PRODUCT_ID))
                .willReturn(Optional.of(product));
        given(loanProductOptionRepository.findByProduct_ProductId(PRODUCT_ID))
                .willReturn(List.of(option1, option2));

        // when
        LoanProductOptionsResponse response = loanProductService.findProductOptions(PRODUCT_ID);

        // then
        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getLoanOptions()).hasSize(2);
        assertThat(response.getLoanOptions().get(0).getPurpose()).isEqualTo(LoanPurpose.WORKING_CAPITAL);
        assertThat(response.getLoanOptions().get(1).getRepaymentMethod()).isEqualTo(RepaymentMethod.EQUAL_PRINCIPAL);
    }

    @Test
    @DisplayName("findProductOptions - 존재하지 않는 상품이면 PRODUCT_NOT_FOUND 예외")
    void findProductOptions_throwsException_whenProductNotFound() {
        // given
        given(loanProductRepository.findById(PRODUCT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanProductService.findProductOptions(PRODUCT_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(LoanErrorCode.PRODUCT_NOT_FOUND);
                });
    }

    // --- 헬퍼 메서드 ---

    private LoanProduct createProduct(Long productId, String productName, String title, Long maxLimit) {
        LoanProduct product;
        try {
            var constructor = LoanProduct.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            product = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("LoanProduct 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(product, "productId", productId);
        ReflectionTestUtils.setField(product, "productName", productName);
        ReflectionTestUtils.setField(product, "title", title);
        ReflectionTestUtils.setField(product, "subtitle", "부제목");
        ReflectionTestUtils.setField(product, "status", ProductStatus.ACTIVE);
        ReflectionTestUtils.setField(product, "minLimit", 1_000_000L);
        ReflectionTestUtils.setField(product, "maxLimit", maxLimit);
        ReflectionTestUtils.setField(product, "minTerm", 12);
        ReflectionTestUtils.setField(product, "maxTerm", 60);
        ReflectionTestUtils.setField(product, "minRate", new BigDecimal("3.50"));
        ReflectionTestUtils.setField(product, "maxRate", new BigDecimal("8.00"));
        ReflectionTestUtils.setField(product, "targetSummary", "소상공인 대상");
        return product;
    }

    private LoanProductOption createOption(Long optionId, LoanPurpose purpose,
                                           RepaymentMethod repaymentMethod, Integer maxTermMonths) {
        LoanProductOption option;
        try {
            var constructor = LoanProductOption.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            option = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("LoanProductOption 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(option, "optionId", optionId);
        ReflectionTestUtils.setField(option, "purpose", purpose);
        ReflectionTestUtils.setField(option, "repaymentMethod", repaymentMethod);
        ReflectionTestUtils.setField(option, "maxTermMonths", maxTermMonths);
        return option;
    }
}
