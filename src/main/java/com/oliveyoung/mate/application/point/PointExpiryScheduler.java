package com.oliveyoung.mate.application.point;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpiryScheduler {

    private final PointService pointService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void expirePoints() {
        pointService.expireAllPoints();
    }
}
