package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.schedule.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyWorkDayScheduler {

    private final ScheduleService scheduleService;

    @Scheduled(cron = "0 0 23 * * SUN", zone = "Asia/Seoul")
    public void generateWeeklyWorkDays() {
        scheduleService.generateNextWeekWorkDays();
    }
}
