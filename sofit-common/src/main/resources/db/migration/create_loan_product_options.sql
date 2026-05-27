-- loan_product_options 테이블 생성
CREATE TABLE loan_product_options (
    option_id        BIGINT       NOT NULL AUTO_INCREMENT,
    product_id       BIGINT       NOT NULL,
    purpose          VARCHAR(20)  NOT NULL,
    repayment_method VARCHAR(20)  NOT NULL,
    max_term_months  INT          NOT NULL,
    created_by       BIGINT,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (option_id),
    FOREIGN KEY (product_id) REFERENCES loan_product(product_id)
);
