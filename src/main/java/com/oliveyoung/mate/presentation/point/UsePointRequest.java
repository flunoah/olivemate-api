package com.oliveyoung.mate.presentation.point;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record UsePointRequest(
    @Min(value = 1, message = "사용 포인트는 1 이상이어야 합니다.")
    long amount,

    String description,

    LocalDate usedAt,  // null이면 오늘 날짜로 처리

    String brand  // 자동완성으로 상품을 선택한 경우에만 채워짐, 자유 입력이면 null
) {}
