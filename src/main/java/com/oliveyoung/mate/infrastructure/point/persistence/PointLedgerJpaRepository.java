package com.oliveyoung.mate.infrastructure.point.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PointLedgerJpaRepository
        extends JpaRepository<PointLedgerJpaEntity, UUID> {

    List<PointLedgerJpaEntity> findByCrewIdOrderByGrantedAtAsc(UUID crewId);

    @Query("""
        SELECT DISTINCT l.crewId FROM PointLedgerJpaEntity l
        WHERE l.ledgerType = 'EARN'
          AND l.remaining > 0
          AND l.expiredAt < :now
        """)
    List<UUID> findDistinctCrewIdsWithExpiringPoints(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PointLedgerJpaEntity l SET l.remaining = :remaining WHERE l.ledgerId = :ledgerId")
    void updateRemaining(@Param("ledgerId") UUID ledgerId, @Param("remaining") Long remaining);

    @Query("""
        SELECT COALESCE(SUM(l.remaining), 0) FROM PointLedgerJpaEntity l
        WHERE l.crewId = :crewId
          AND l.ledgerType = 'EARN'
          AND l.remaining > 0
          AND l.expiredAt >= :from
          AND l.expiredAt < :to
        """)
    Long sumRemainingByCrewIdAndExpiredAtBetween(
        @Param("crewId") UUID crewId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);
}