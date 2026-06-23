package com.oliveyoung.mate.infrastructure.point.persistence;

import com.oliveyoung.mate.domain.point.model.Point;
import com.oliveyoung.mate.domain.point.model.PointLedger;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PointMapper {

    // JPA 엔티티 → 도메인 객체 변환
    public Point toDomain(PointAccountJpaEntity account,
                          List<PointLedgerJpaEntity> ledgers) {
        CrewId crewId = CrewId.of(account.getCrewId());
        Money balance = Money.of(account.getBalance());
        List<PointLedger> domainLedgers = ledgers.stream()
            .map(this::toLedgerDomain)
            .toList();
        return new Point(crewId, balance, domainLedgers);
    }

    // 원장 JPA 엔티티 → 도메인 객체 변환
    // 반드시 reconstruct()를 사용해야 ledgerId·remaining이 DB 값으로 복원됨
    public PointLedger toLedgerDomain(PointLedgerJpaEntity entity) {
        return PointLedger.reconstruct(
            entity.getLedgerId(),
            CrewId.of(entity.getCrewId()),
            entity.getWorkDayId(),
            entity.getTxId(),
            PointLedger.LedgerType.valueOf(entity.getLedgerType().name()),
            Money.of(entity.getAmount()),
            Money.of(entity.getRemaining()),
            entity.getGrantedAt(),
            entity.getExpiredAt(),
            entity.getCreatedAt(),
            entity.getDescription()
        );
    }

    // 도메인 객체 → JPA 엔티티 변환
    public PointLedgerJpaEntity toJpa(PointLedger ledger) {
        return PointLedgerJpaEntity.builder()
            .ledgerId(ledger.getLedgerId())
            .crewId(ledger.getCrewId().id())
            .workDayId(ledger.getWorkDayId())
            .txId(ledger.getTxId())
            .ledgerType(PointLedgerJpaEntity.LedgerType
                .valueOf(ledger.getType().name()))
            .amount(ledger.getAmount().amount())
            .remaining(ledger.getRemaining().amount())
            .grantedAt(ledger.getGrantedAt())
            .expiredAt(ledger.getExpiredAt())
            .description(ledger.getDescription())
            .build();
    }
}