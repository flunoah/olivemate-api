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

    // 원장 전체 로드 없이 만료 예정 포인트 합산
    Money sumExpiringBetween(CrewId crewId, LocalDateTime from, LocalDateTime to);

    // 이번달 적립/사용 합산
    Money sumByTypeAndPeriod(CrewId crewId, String type, LocalDateTime from, LocalDateTime to);
}