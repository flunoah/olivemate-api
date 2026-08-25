package com.oliveyoung.mate.infrastructure.point.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PointLedgerJpaRepository
        extends JpaRepository<PointLedgerJpaEntity, UUID> {

    List<PointLedgerJpaEntity> findByCrewIdOrderByGrantedAtAsc(UUID crewId);

    @Query("""
        SELECT DISTINCT l.crewId FROM PointLedgerJpaEntity l
        WHERE l.ledgerType IN :types
          AND l.remaining > 0
          AND l.expiredAt IS NOT NULL
          AND l.expiredAt < :now
        """)
    List<UUID> findDistinctCrewIdsWithExpiringPoints(
        @Param("types") Collection<PointLedgerJpaEntity.LedgerType> types,
        @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PointLedgerJpaEntity l SET l.remaining = :remaining WHERE l.ledgerId = :ledgerId")
    void updateRemaining(@Param("ledgerId") UUID ledgerId, @Param("remaining") Long remaining);

    // 잔액 조회(PointService.getBalance())에 필요한 5개 통계를 한 번의 왕복으로 집계.
    // CASE WHEN 하나로 합치면 크루의 원장 전체를 풀스캔하게 되어 기존 인덱스
    // (idx_ledger_crew_type_expired / idx_ledger_crew_granted)를 못 타므로, 대신
    // 서브쿼리 5개로 쪼갠다 — 각 서브쿼리는 예전 개별 쿼리와 동일한 WHERE 조건이라
    // 인덱스를 그대로 타면서, 왕복은 SELECT 한 번(서브쿼리 5개가 서버 안에서 처리)으로 끝난다.
    // remaining(잔여 차감가능액)과 amount(원거래액)를 섞지 않도록 expiring 계열은
    // remaining을, monthly 계열은 amount를 쓰고, expired_at 기준(expiring)과
    // granted_at 기준(monthly)도 서로 다른 컬럼임을 서브쿼리마다 명시한다.
    // ledger_type 값은 문자열 리터럴로 박지 않고 PointRepositoryImpl에서 LedgerType.name()으로
    // 넘긴다 — enum 이름이 바뀌면 그 참조가 컴파일 에러로 걸리게 하기 위함(리터럴이었다면
    // enum을 리네임해도 이 쿼리는 조용히 아무것도 못 찾아 0을 반환했을 것).
    @Query(value = """
        SELECT
            COALESCE((SELECT SUM(remaining) FROM point_ledger
                      WHERE crew_id = :crewId AND ledger_type IN (:earnTypes) AND remaining > 0
                        AND expired_at IS NOT NULL AND expired_at >= :now AND expired_at < :in7Days), 0) AS "expiringIn7Days",
            COALESCE((SELECT SUM(remaining) FROM point_ledger
                      WHERE crew_id = :crewId AND ledger_type IN (:earnTypes) AND remaining > 0
                        AND expired_at IS NOT NULL AND expired_at >= :now AND expired_at < :in30Days), 0) AS "expiringIn30Days",
            COALESCE((SELECT SUM(amount) FROM point_ledger
                      WHERE crew_id = :crewId AND ledger_type = :earnType
                        AND granted_at >= :monthStart AND granted_at < :monthEnd), 0) AS "monthlyEarned",
            COALESCE((SELECT SUM(amount) FROM point_ledger
                      WHERE crew_id = :crewId AND ledger_type = :useType
                        AND granted_at >= :monthStart AND granted_at < :monthEnd), 0) AS "monthlyUsed",
            COALESCE((SELECT SUM(remaining) FROM point_ledger
                      WHERE crew_id = :crewId AND ledger_type IN (:earnTypes) AND remaining > 0
                        AND expired_at IS NOT NULL AND expired_at >= :now AND expired_at < :monthEnd), 0) AS "monthlyExpiring"
        """, nativeQuery = true)
    BalanceAggregateRow findBalanceAggregates(
        @Param("crewId") UUID crewId,
        @Param("earnTypes") Collection<String> earnTypes,
        @Param("earnType") String earnType,
        @Param("useType") String useType,
        @Param("now") LocalDateTime now,
        @Param("in7Days") LocalDateTime in7Days,
        @Param("in30Days") LocalDateTime in30Days,
        @Param("monthStart") LocalDateTime monthStart,
        @Param("monthEnd") LocalDateTime monthEnd);

    interface BalanceAggregateRow {
        Long getExpiringIn7Days();
        Long getExpiringIn30Days();
        Long getMonthlyEarned();
        Long getMonthlyUsed();
        Long getMonthlyExpiring();
    }

    @Modifying
    @Query("DELETE FROM PointLedgerJpaEntity l WHERE l.txId = :txId")
    void deleteByTxId(@Param("txId") UUID txId);

    @Modifying
    @Query(value = """
        UPDATE point_ledger
           SET expired_at = granted_at + INTERVAL '21 days'
         WHERE ledger_type = 'INIT'
           AND expired_at IS NULL
        """, nativeQuery = true)
    int backfillInitExpiredAt();

    @Query("""
        SELECT l.crewId AS crewId, COALESCE(SUM(l.remaining), 0) AS amount
        FROM PointLedgerJpaEntity l
        WHERE l.ledgerType IN :types
          AND l.remaining > 0
          AND l.expiredAt >= :from
          AND l.expiredAt < :to
        GROUP BY l.crewId
        """)
    List<ExpiringAmountRow> findExpiringAmountsBetween(
        @Param("types") Collection<PointLedgerJpaEntity.LedgerType> types,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    interface ExpiringAmountRow {
        UUID getCrewId();
        Long getAmount();
    }
}