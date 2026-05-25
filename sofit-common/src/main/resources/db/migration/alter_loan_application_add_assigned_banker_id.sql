-- 담당 은행원 자동 배정을 위한 컬럼 추가
ALTER TABLE loan_application
ADD COLUMN assigned_banker_id BIGINT NULL COMMENT '담당 은행원 userId';
