package com.oliveyoung.mate.infrastructure.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    /**
     * shedlock 테이블(V9)은 JPA 엔티티가 아니라 ddl-auto=validate로 부재가 걸러지지 않는다.
     * 락 획득 실패는 shedlock 라이브러리가 "락 못 잡음"과 동일하게 처리해 스케줄러 본문을
     * 조용히 스킵시키므로(텔레그램 알림도 안 감), 기동 시점에 직접 조회해 마이그레이션
     * 누락을 기동 실패로 드러낸다.
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        new JdbcTemplate(dataSource).execute("SELECT 1 FROM shedlock WHERE 1 = 0");
        return new JdbcTemplateLockProvider(dataSource);
    }
}
