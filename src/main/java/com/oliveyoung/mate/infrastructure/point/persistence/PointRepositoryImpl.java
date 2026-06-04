package com.oliveyoung.mate.infrastructure.point.persistence;

import com.oliveyoung.mate.domain.point.model.Point;
import com.oliveyoung.mate.domain.point.model.PointLedger;
import com.oliveyoung.mate.domain.point.repository.PointRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {

    private final PointAccountJpaRepository accountJpaRepo;
    private final PointLedgerJpaRepository  ledgerJpaRepo;
    private final PointMapper               mapper;

    @Override
    public Optional<Point> findByCrewId(CrewId crewId) {
        return accountJpaRepo.findByCrewId(crewId.id()).map(account -> {
            List<PointLedgerJpaEntity> ledgers =
                ledgerJpaRepo.findByCrewIdOrderByGrantedAtAsc(crewId.id());
            return mapper.toDomain(account, ledgers);
        });
    }

    @Override
    public Point save(Point point) {
        // point_account upsert
        PointAccountJpaEntity account = accountJpaRepo
            .findByCrewId(point.getCrewId().id())
            .orElseGet(() -> PointAccountJpaEntity.builder()
                .accountId(UUID.randomUUID())
                .crewId(point.getCrewId().id())
                .balance(0L)
                .build());
        account.updateBalance(point.getBalance().amount());
        accountJpaRepo.save(account);

        // 신규 원장 batch INSERT (이번 세션에서 추가된 것만)
        List<PointLedgerJpaEntity> newEntities = point.getNewLedgers().stream()
            .map(mapper::toJpa)
            .collect(Collectors.toList());
        if (!newEntities.isEmpty()) {
            ledgerJpaRepo.saveAll(newEntities);
        }

        // remaining이 변경된 기존 원장 targeted UPDATE (보통 1~5건)
        point.getDirtyLedgers()
            .forEach(l -> ledgerJpaRepo.updateRemaining(l.getLedgerId(), l.getRemaining().amount()));

        return point;
    }

    @Override
    public List<CrewId> findAllCrewIdsWithExpiringPoints() {
        return ledgerJpaRepo
            .findDistinctCrewIdsWithExpiringPoints(LocalDateTime.now())
            .stream()
            .map(CrewId::of)
            .toList();
    }

    @Override
    public Optional<Money> findBalanceByCrewId(CrewId crewId) {
        return accountJpaRepo.findByCrewId(crewId.id())
            .map(a -> Money.of(a.getBalance()));
    }

    @Override
    public Money sumExpiringBetween(CrewId crewId, LocalDateTime from, LocalDateTime to) {
        Long sum = ledgerJpaRepo.sumRemainingByCrewIdAndExpiredAtBetween(
            crewId.id(), from, to);
        return sum != null ? Money.of(sum) : Money.zero();
    }

    @Override
    public Money sumByTypeAndPeriod(CrewId crewId, String type, LocalDateTime from, LocalDateTime to) {
        Long sum = ledgerJpaRepo.sumAmountByCrewIdAndTypeAndCreatedAtBetween(
            crewId.id(), type, from, to);
        return sum != null ? Money.of(sum) : Money.zero();
    }

    @Override
    public Optional<PointLedger> findLedgerById(UUID ledgerId) {
        return ledgerJpaRepo.findById(ledgerId).map(mapper::toLedgerDomain);
    }

    @Override
    public void deleteLedgersByTxId(UUID txId) {
        ledgerJpaRepo.deleteByTxId(txId);
    }
}