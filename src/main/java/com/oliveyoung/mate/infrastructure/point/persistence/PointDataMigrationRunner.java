package com.oliveyoung.mate.infrastructure.point.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointDataMigrationRunner implements CommandLineRunner {

    private final PointLedgerJpaRepository ledgerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        int updated = ledgerRepository.backfillInitExpiredAt();
        if (updated > 0) {
            log.info("[Migration] INIT 포인트 만료일 소급 적용 완료: {}건 (granted_at + 21일)", updated);
        }
    }
}
