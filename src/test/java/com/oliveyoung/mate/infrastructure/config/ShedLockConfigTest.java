package com.oliveyoung.mate.infrastructure.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ShedLock 분산락(ShedLockConfig가 실제로 사용하는 JdbcTemplateLockProvider)이
 * 동시 실행을 실제로 막는지 검증. 실제 운영과 같은 DB 엔진(Postgres, V9의 shedlock 테이블)을 사용한다
 * — 이 테스트는 dev DB 연결이 필요해 CI에서는 제외된다(-PciSkipContextTest, MateApplicationTests와 동일 사유).
 * 롤링 배포 중 신/구 인스턴스가 겹쳐 같은 크론(예: PointGrantScheduler)이 동시에 도는 상황과 동일하다.
 */
class ShedLockConfigTest {

    private static final String LOCK_NAME = "ShedLockConfigTest_lock";

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        dataSource = new SimpleDriverDataSource(new Driver(),
            System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", LOCK_NAME);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", LOCK_NAME);
    }

    @Test
    @DisplayName("같은 이름의 락을 두 스레드가 동시에 시도하면 하나만 실제로 실행된다")
    void only_one_thread_runs_under_the_same_lock() throws InterruptedException {
        LockingTaskExecutor executor = new DefaultLockingTaskExecutor(new JdbcTemplateLockProvider(dataSource));
        AtomicInteger executionCount = new AtomicInteger();

        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        Runnable job = () -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor.executeWithLock((Runnable) executionCount::incrementAndGet,
                new LockConfiguration(Instant.now(), LOCK_NAME,
                    Duration.ofMinutes(30), Duration.ZERO));
        };

        for (int i = 0; i < threads; i++) {
            pool.submit(job);
        }
        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(executionCount.get()).isEqualTo(1);
    }
}
