package com.oliveyoung.mate.presentation.point;

import jakarta.validation.constraints.Min;

public record UsePointRequest(
    @Min(value = 1, message = "사용 포인트는 1 이상이어야 합니다.")
    long amount,

    String description
) {}
