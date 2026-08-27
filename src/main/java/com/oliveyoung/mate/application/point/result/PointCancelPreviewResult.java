package com.oliveyoung.mate.application.point.result;

import java.time.LocalDateTime;
import java.util.List;

public record PointCancelPreviewResult(List<Line> lines, long resultingBalance) {
    // expiredAt == null이면 "만료 없음"(초기 포인트)
    public record Line(LocalDateTime expiredAt, long remainingBefore, long amount) {}
}
