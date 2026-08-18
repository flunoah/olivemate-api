package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.presentation.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpiryScheduler {

    private final PointService pointService;
    private final TelegramNotifier telegramNotifier;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void expirePoints() {
        try {
            telegramNotifier.sendJobReport(pointService.expireAllPoints());
        } catch (Exception e) {
            log.error("[Admin Cron] 포인트 만료 스케줄러 실패", e);
            telegramNotifier.sendSchedulerError("포인트 만료 (PointExpiryScheduler)", e);
        }
    }
}
