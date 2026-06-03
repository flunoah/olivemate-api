package com.oliveyoung.mate.presentation.point;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CancelUseRequest(
    @NotNull UUID ledgerId,
    @NotNull UUID crewId
) {}
