package com.oliveyoung.mate.infrastructure.point.persistence;

import com.oliveyoung.mate.domain.point.model.Point;
import com.oliveyoung.mate.domain.point.model.PointLedger;
import com.oliveyoung.mate.domain.point.repository.PointRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {

    private final PointAccountJpaRepository    accountJpaRepo;
    private final PointLedgerJpaRepository     ledgerJpaRepo;
    private final PointUseRequestJpaRepository useRequestJpaRepo;
    private final PointMapper                  mapper;

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

    private static final Set<PointLedgerJpaEntity.LedgerType> EARN_TYPES =
        Set.of(PointLedgerJpaEntity.LedgerType.EARN, PointLedgerJpaEntity.LedgerType.INIT);

    @Override
    public List<CrewId> findAllCrewIdsWithExpiringPoints() {
        return ledgerJpaRepo
            .findDistinctCrewIdsWithExpiringPoints(EARN_TYPES, LocalDateTime.now())
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
            crewId.id(), EARN_TYPES, from, to);
        return sum != null ? Money.of(sum) : Money.zero();
    }

    @Override
    public Money sumByTypeAndPeriod(CrewId crewId, String type, LocalDateTime from, LocalDateTime to) {
        Long sum = ledgerJpaRepo.sumAmountByCrewIdAndTypeAndGrantedAtBetween(
            crewId.id(), PointLedgerJpaEntity.LedgerType.valueOf(type), from, to);
        return sum != null ? Money.of(sum) : Money.zero();
    }

    @Override
    public List<ExpiringReminder> findExpiringAmountsBetween(LocalDateTime from, LocalDateTime to) {
        return ledgerJpaRepo.findExpiringAmountsBetween(EARN_TYPES, from, to).stream()
            .map(row -> new ExpiringReminder(CrewId.of(row.getCrewId()), Money.of(row.getAmount())))
            .toList();
    }

    @Override
    public Optional<PointLedger> findLedgerById(UUID ledgerId) {
        return ledgerJpaRepo.findById(ledgerId).map(mapper::toLedgerDomain);
    }

    @Override
    public void deleteLedgersByTxId(UUID txId) {
        ledgerJpaRepo.deleteByTxId(txId);
    }

    // REQUIRES_NEW — UNIQUE 위반 시 Postgres가 트랜잭션 전체를 abort 상태로 만들기 때문에,
    // use()의 메인 트랜잭션과 분리된 커넥션에서 실패시켜야 이후 findByCrewId 등 후속 쿼리가 멀쩡하다.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean registerUseRequestIfAbsent(CrewId crewId, UUID idempotencyKey, UUID txId) {
        try {
            useRequestJpaRepo.saveAndFlush(PointUseRequestJpaEntity.builder()
                .id(UUID.randomUUID())
                .crewId(crewId.id())
                .idempotencyKey(idempotencyKey)
                .txId(txId)
                .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public Optional<UUID> findTxIdByIdempotencyKey(CrewId crewId, UUID idempotencyKey) {
        return useRequestJpaRepo.findByCrewIdAndIdempotencyKey(crewId.id(), idempotencyKey)
            .map(PointUseRequestJpaEntity::getTxId);
    }
}