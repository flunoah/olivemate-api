package com.oliveyoung.mate.presentation.point;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GrantPointManualRequest(
    @NotNull UUID crewId,
    @NotNull LocalDate workDate
) {}
