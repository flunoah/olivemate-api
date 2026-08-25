package com.oliveyoung.mate.infrastructure.point.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.oliveyoung.mate.infrastructure.point.persistence.PointLedgerJpaEntity.LedgerType.EARN;
import static com.oliveyoung.mate.infrastructure.point.persistence.PointLedgerJpaEntity.LedgerType.EXPIRE;
import static com.oliveyoung.mate.infrastructure.point.persistence.PointLedgerJpaEntity.LedgerType.INIT;
import static com.oliveyoung.mate.infrastructure.point.persistence.PointLedgerJpaEntity.LedgerType.USE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * getBalance()용 6개 쿼리를 findBalanceAggregates() 단일 쿼리로 합치면서
 * 실제로 우려됐던 버그 시나리오(remaining/amount 혼용, granted_at/expired_at 혼용,
 * 구간 중첩, ledgerType 필터 누락, 신규 크루 NPE)를 각각 검증한다.
 *
 * DB를 직접 쳐야만 CASE WHEN 로직의 정합성을 검증할 수 있어 이 프로젝트에서
 * 흔치 않은 @SpringBootTest를 사용한다(로컬 .env의 실제 Postgres 필요).
 * Spring Boot 4.0.7에는 @DataJpaTest/@AutoConfigureTestDatabase가 없어(제거됨,
 * spring-boot-test-autoconfigure 4.0.7 jar에 해당 클래스가 없는 것을 직접 확인)
 * 전체 컨텍스트를 띄우는 대신 클래스에 @Transactional을 붙여 테스트마다 자동
 * 롤백되게 한다.
 */
@SpringBootTest
@Transactional
class PointLedgerJpaRepositoryBalanceAggregateTest {

    @Autowired
    private PointLedgerJpaRepository repository;

    private static final LocalDateTime NOW         = LocalDateTime.of(2026, 8, 15, 10, 0);
    private static final LocalDateTime MONTH_START  = LocalDateTime.of(2026, 8, 1, 0, 0);
    private static final LocalDateTime MONTH_END    = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime IN_7_DAYS    = NOW.plusDays(7);
    private static final LocalDateTime IN_30_DAYS   = NOW.plusDays(30);
    private static final List<String> EARN_TYPE_NAMES = List.of(EARN.name(), INIT.name());

    private PointLedgerJpaRepository.BalanceAggregateRow aggregate(UUID crewId) {
        return repository.findBalanceAggregates(
            crewId, EARN_TYPE_NAMES, EARN.name(), USE.name(), NOW, IN_7_DAYS, IN_30_DAYS, MONTH_START, MONTH_END);
    }

    private PointLedgerJpaEntity ledger(UUID crewId, PointLedgerJpaEntity.LedgerType type,
                                         long amount, long remaining,
                                         LocalDateTime grantedAt, LocalDateTime expiredAt) {
        return PointLedgerJpaEntity.builder()
            .ledgerId(UUID.randomUUID())
            .crewId(crewId)
            .ledgerType(type)
            .amount(amount)
            .remaining(remaining)
            .grantedAt(grantedAt)
            .expiredAt(expiredAt)
            .build();
    }

    @Test
    @DisplayName("소멸예정 통계는 amount가 아니라 remaining(잔여 차감가능액)을 합산한다")
    void expiringUsesRemainingNotAmount() {
        UUID crewId = UUID.randomUUID();
        // 1000P 적립분 중 600P를 이미 써서 remaining=400만 남음
        repository.save(ledger(crewId, EARN, 1000, 400, NOW.minusDays(1), NOW.plusDays(5)));

        var row = aggregate(crewId);

        assertThat(row.getExpiringIn7Days()).isEqualTo(400L);
    }

    @Test
    @DisplayName("이번달 통계는 grantedAt 기준, 소멸예정 통계는 expiredAt 기준으로 각각 필터링된다")
    void monthlyStatsFilterByGrantedAtNotExpiredAt() {
        UUID crewId = UUID.randomUUID();
        // 이번달 사용 2000P (grantedAt=이번달)
        repository.save(ledger(crewId, USE, 2000, 0, NOW, null));
        // 작년에 적립했지만 만료일만 이번달 7일 이내인 EARN 3000P
        // -> expiring 통계엔 잡혀야 하고, monthlyEarned엔 잡히면 안 됨
        repository.save(ledger(crewId, EARN, 3000, 3000, LocalDateTime.of(2025, 1, 1, 0, 0), NOW.plusDays(3)));

        var row = aggregate(crewId);

        assertThat(row.getMonthlyUsed()).isEqualTo(2000L);
        assertThat(row.getMonthlyEarned()).isEqualTo(0L);
        assertThat(row.getExpiringIn7Days()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("7일/30일/이번달 소멸예정 구간은 서로 겹치지 않고 독립적으로 계산된다")
    void sevenDayThirtyDayMonthEndBucketsAreIndependent() {
        UUID crewId = UUID.randomUUID();
        // 9/10 만료: 7일 밖(8/22 이후), 이번달 밖(9/1 이후), 30일 안(9/14 이내)
        repository.save(ledger(crewId, EARN, 500, 500, NOW.minusDays(1), LocalDateTime.of(2026, 9, 10, 0, 0)));

        var row = aggregate(crewId);

        assertThat(row.getExpiringIn7Days()).isEqualTo(0L);
        assertThat(row.getMonthlyExpiring()).isEqualTo(0L);
        assertThat(row.getExpiringIn30Days()).isEqualTo(500L);
    }

    @Test
    @DisplayName("INIT 원장은 소멸예정 통계엔 포함되지만 이번달 적립(EARN 전용) 통계엔 포함되지 않는다")
    void initTypeCountsTowardExpiringButNotMonthlyEarned() {
        UUID crewId = UUID.randomUUID();
        repository.save(ledger(crewId, INIT, 1000, 1000, NOW, NOW.plusDays(2)));

        var row = aggregate(crewId);

        assertThat(row.getExpiringIn7Days()).isEqualTo(1000L);
        assertThat(row.getMonthlyEarned()).isEqualTo(0L);
    }

    @Test
    @DisplayName("원장이 하나도 없는 신규 크루는 NPE 없이 전부 0을 반환한다")
    void newCrewWithNoLedgerReturnsZeroNotNull() {
        UUID crewId = UUID.randomUUID();

        var row = aggregate(crewId);

        assertThat(row.getExpiringIn7Days()).isEqualTo(0L);
        assertThat(row.getExpiringIn30Days()).isEqualTo(0L);
        assertThat(row.getMonthlyEarned()).isEqualTo(0L);
        assertThat(row.getMonthlyUsed()).isEqualTo(0L);
        assertThat(row.getMonthlyExpiring()).isEqualTo(0L);
    }

    @Test
    @DisplayName("EXPIRE 원장은 remaining 값과 무관하게 어떤 통계에도 잡히지 않는다")
    void expireTypeNeverCountsRegardlessOfRemaining() {
        UUID crewId = UUID.randomUUID();
        // 정상적으로는 EXPIRE 원장의 remaining은 항상 0이지만, 쿼리가 타입 필터를
        // 제대로 걸고 있는지 보려고 일부러 remaining>0인 비정상 데이터로 검증한다
        repository.save(ledger(crewId, EXPIRE, 700, 700, NOW, NOW.plusDays(1)));

        var row = aggregate(crewId);

        assertThat(row.getExpiringIn7Days()).isEqualTo(0L);
        assertThat(row.getMonthlyEarned()).isEqualTo(0L);
        assertThat(row.getMonthlyUsed()).isEqualTo(0L);
    }
}
