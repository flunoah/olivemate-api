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
public class WeeklyWorkDayScheduler {

    private final ScheduleService scheduleService;
    private final TelegramNotifier telegramNotifier;

    @Scheduled(cron = "0 0 23 * * SUN", zone = "Asia/Seoul")
    public void generateWeeklyWorkDays() {
        try {
            telegramNotifier.sendJobReport(scheduleService.generateNextWeekWorkDays());
        } catch (Exception e) {
            log.error("[Admin Cron] 주간 근무일 생성 스케줄러 실패", e);
            telegramNotifier.sendSchedulerError("주간 근무일 생성 (WeeklyWorkDayScheduler)", e);
        }
    }
}
