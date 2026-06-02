package com.oliveyoung.mate.application.point.command;

import java.time.LocalDate;
import java.util.UUID;

public record EarnPointCommand(
    UUID crewId,
    UUID workDayId,
    LocalDate workDate   // 실제 근무일 — 스케줄러 실행 시점이 아닌 이 날짜 기준으로 만료일 계산
) {}