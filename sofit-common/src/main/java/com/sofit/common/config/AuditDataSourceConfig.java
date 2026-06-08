package com.sofit.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 감사 로그 전용 DataSource / JdbcTemplate.
 *
 * <p><b>직무 분리(권한 레벨)</b>: 앱 계정(sofit, 풀 DML)과 분리된 audit_writer 계정으로 연결한다.
 * audit_writer 에는 INSERT/SELECT 권한만 부여되어, 앱이 탈취돼도 과거 감사 로그를 변조할 수 없다.
 * (계정·GRANT 는 scripts/sql/audit_log.sql 참고)</p>
 *
 * <p><b>설계 주의</b>: DataSource 타입 빈을 등록하면 Spring Boot 의 기본 DataSource 자동구성이
 * 비활성화된다. 이를 피하려고 audit DataSource 는 빈으로 노출하지 않고 내부에서 직접 생성하여
 * {@code auditJdbcTemplate} 만 빈으로 공개한다. 풀 종료는 {@link DisposableBean}으로 처리.</p>
 */
@Configuration
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditDataSourceConfig implements DisposableBean {

    private HikariDataSource auditDataSource;

    @Bean
    public JdbcTemplate auditJdbcTemplate(
            @Value("${audit.datasource.url:${spring.datasource.url}}") String url,
            @Value("${audit.datasource.username:audit_writer}") String username,
            @Value("${audit.datasource.password:}") String password) {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(3);
        config.setPoolName("audit-pool");
        config.setReadOnly(false);

        this.auditDataSource = new HikariDataSource(config);
        return new JdbcTemplate(this.auditDataSource);
    }

    @Override
    public void destroy() {
        if (auditDataSource != null && !auditDataSource.isClosed()) {
            auditDataSource.close();
        }
    }
}
