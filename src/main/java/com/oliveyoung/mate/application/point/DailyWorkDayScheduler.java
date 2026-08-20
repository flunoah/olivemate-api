package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.schedule.ScheduleService;
import com.oliveyoung.mate.presentation.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyWorkDayScheduler {

    private final ScheduleService scheduleService;
    private final TelegramNotifier telegramNotifier;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void generateDailyWorkDays() {
        try {
            telegramNotifier.sendJobReport(scheduleService.generateTodayWorkDays());
        } catch (Exception e) {
            log.error("[Daily Cron] 일일 근무일 생성 스케줄러 실패", e);
            telegramNotifier.sendSchedulerError("일일 근무일 생성 (DailyWorkDayScheduler)", e);
        }
    }
}
