-- 우리 사장님 대출 (product_id = 1)
INSERT INTO loan_product_options (product_id, purpose, repayment_method, max_term_months, created_by, created_at, updated_at) VALUES
(1, 'WORKING_CAPITAL',  'BULLET',          12,  2, NOW(), NOW()),
(1, 'WORKING_CAPITAL',  'EQUAL_PRINCIPAL',  60,  2, NOW(), NOW()),
(1, 'WORKING_CAPITAL',  'EQUAL_PAYMENT',    60,  2, NOW(), NOW()),
(1, 'FACILITY_CAPITAL', 'BULLET',           36,  2, NOW(), NOW()),
(1, 'FACILITY_CAPITAL', 'EQUAL_PRINCIPAL', 240,  2, NOW(), NOW()),
(1, 'FACILITY_CAPITAL', 'EQUAL_PAYMENT',   240,  2, NOW(), NOW());

-- 우리 Oh!(5) 클릭 대출 (product_id = 2)
INSERT INTO loan_product_options (product_id, purpose, repayment_method, max_term_months, created_by, created_at, updated_at) VALUES
(2, 'WORKING_CAPITAL', 'BULLET',          12, 2, NOW(), NOW()),
(2, 'WORKING_CAPITAL', 'EQUAL_PRINCIPAL',  60, 2, NOW(), NOW()),
(2, 'WORKING_CAPITAL', 'EQUAL_PAYMENT',    60, 2, NOW(), NOW());
