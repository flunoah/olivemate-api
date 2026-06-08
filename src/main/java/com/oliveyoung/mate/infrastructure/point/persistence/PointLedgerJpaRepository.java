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

    @Query("""
        SELECT COALESCE(SUM(l.remaining), 0) FROM PointLedgerJpaEntity l
        WHERE l.crewId = :crewId
          AND l.ledgerType IN :types
          AND l.remaining > 0
          AND l.expiredAt IS NOT NULL
          AND l.expiredAt >= :from
          AND l.expiredAt < :to
        """)
    Long sumRemainingByCrewIdAndExpiredAtBetween(
        @Param("crewId") UUID crewId,
        @Param("types") Collection<PointLedgerJpaEntity.LedgerType> types,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(l.amount), 0) FROM PointLedgerJpaEntity l
        WHERE l.crewId = :crewId
          AND l.ledgerType = :type
          AND l.createdAt >= :from
          AND l.createdAt < :to
        """)
    Long sumAmountByCrewIdAndTypeAndCreatedAtBetween(
        @Param("crewId") UUID crewId,
        @Param("type") PointLedgerJpaEntity.LedgerType type,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

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
}