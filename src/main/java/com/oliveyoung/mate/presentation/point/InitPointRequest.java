package com.oliveyoung.mate.presentation.point;

import jakarta.validation.constraints.Min;

public record InitPointRequest(
    @Min(value = 1, message = "초기 포인트는 1 이상이어야 합니다.")
    long amount
) {}
