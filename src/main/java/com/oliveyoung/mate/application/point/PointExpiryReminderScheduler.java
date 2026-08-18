package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.presentation.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpiryReminderScheduler {

    private final PointService pointService;
    private final TelegramNotifier telegramNotifier;

    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void remindExpiringPoints() {
        try {
            telegramNotifier.sendJobReport(pointService.remindExpiringPoints());
        } catch (Exception e) {
            log.error("[Admin Cron] 소멸 임박 알림 스케줄러 실패", e);
            telegramNotifier.sendSchedulerError("소멸 임박 알림 (PointExpiryReminderScheduler)", e);
        }
    }
}
