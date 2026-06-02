package com.oliveyoung.mate.presentation.attendance;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterWorkDayRequest(
    @NotNull(message = "crewId는 필수입니다.")
    UUID crewId,

    @NotNull(message = "근무일은 필수입니다.")
    LocalDate workDate
) {}
