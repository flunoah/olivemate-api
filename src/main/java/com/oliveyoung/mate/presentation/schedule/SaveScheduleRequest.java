package com.oliveyoung.mate.presentation.schedule;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaveScheduleRequest(
    @NotNull(message = "crewId는 필수입니다.")
    UUID crewId,

    @NotEmpty(message = "근무 요일을 하나 이상 선택해주세요.")
    List<Integer> daysOfWeek,

    @NotNull(message = "시작일은 필수입니다.")
    LocalDate startDate,

    LocalDate endDate     // null = 무기한
) {}
