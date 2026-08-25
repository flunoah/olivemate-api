package com.oliveyoung.mate.domain.point.repository;

import com.oliveyoung.mate.domain.point.model.Point;
import com.oliveyoung.mate.domain.point.model.PointLedger;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointRepository {
    Optional<Point> findByCrewId(CrewId crewId);
    Point save(Point point);
    List<CrewId> findAllCrewIdsWithExpiringPoints();

    Optional<PointLedger> findLedgerById(UUID ledgerId);
    void deleteLedgersByTxId(UUID txId);

    // 원장 전체 로드 없이 잔액만 조회
    Optional<Money> findBalanceByCrewId(CrewId crewId);

    // 잔액 조회 화면에 필요한 만료예정/이번달 통계 5종을 단일 쿼리로 집계
    // (예전엔 sumExpiringBetween 3회 + sumByTypeAndPeriod 2회, 총 5회 왕복이었음)
    BalanceAggregates findBalanceAggregates(CrewId crewId, LocalDateTime now,
                                             LocalDateTime monthStart, LocalDateTime monthEnd);

    record BalanceAggregates(
        Money expiringIn7Days,
        Money expiringIn30Days,
        Money monthlyEarned,
        Money monthlyUsed,
        Money monthlyExpiring
    ) {}

    // 만료 예정 알림 배치용 — 크루별 만료 예정 합계 일괄 조회
    List<ExpiringReminder> findExpiringAmountsBetween(LocalDateTime from, LocalDateTime to);

    record ExpiringReminder(CrewId crewId, Money amount) {}
}