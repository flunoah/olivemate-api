package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.presentation.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointGrantScheduler {

    private final PointService pointService;
    private final TelegramNotifier telegramNotifier;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void grantPoints() {
        try {
            telegramNotifier.sendJobReport(pointService.grantPointsForAll());
        } catch (Exception e) {
            // 스케줄러 스레드의 예외는 GlobalExceptionHandler가 잡지 못하므로 여기서 직접 알린다
            log.error("[Admin Cron] 포인트 적립 스케줄러 실패", e);
            telegramNotifier.sendSchedulerError("포인트 적립 (PointGrantScheduler)", e);
        }
    }
}
