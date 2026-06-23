package com.oliveyoung.mate.application.point.result;

import com.oliveyoung.mate.domain.point.model.PointLedger;
import java.time.LocalDateTime;

public record LedgerHistoryResult(
    PointLedger.LedgerType ledgerType,
    long amount,
    long remaining,
    LocalDateTime grantedAt,
    LocalDateTime expiredAt,
    LocalDateTime createdAt,
    String description
) {}