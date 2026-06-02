package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.attendance.AttendanceService;
import com.oliveyoung.mate.application.attendance.command.RegisterWorkDayCommand;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.schedule.repository.CrewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyWorkDayScheduler {

    private final CrewScheduleRepository scheduleRepository;
    private final AttendanceService      attendanceService;
    private final WorkDayRepository      workDayRepository;

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    public void generateWeeklyWorkDays() {
        log.info("[Scheduler] 주간 근무일 자동 생성 시작");

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);

        scheduleRepository.findAllActive().forEach(schedule -> {
            if (schedule.getStartDate().isAfter(monday)) return;

            schedule.getDaysOfWeek().forEach(dayOfWeek -> {
                LocalDate workDate = monday.plusDays(
                    dayOfWeek == 0 ? 6 : dayOfWeek - 1
                );

                if (schedule.getEndDate() != null && workDate.isAfter(schedule.getEndDate())) {
                    log.info("[Scheduler] 스케줄 종료일 초과 스킵. crewId={} date={}",
                        schedule.getCrewId(), workDate);
                    return;
                }

                boolean isAbsent = workDayRepository
                    .findByCrewIdAndWorkDate(schedule.getCrewId(), workDate)
                    .map(w -> w.isSkipped())
                    .orElse(false);

                if (isAbsent) {
                    log.info("[Scheduler] 결근 처리 날짜 스킵. crewId={} date={}",
                        schedule.getCrewId(), workDate);
                    return;
                }

                try {
                    attendanceService.registerWorkDay(
                        new RegisterWorkDayCommand(schedule.getCrewId(), workDate)
                    );
                    log.info("[Scheduler] 근무일 생성. crewId={} date={}",
                        schedule.getCrewId(), workDate);
                } catch (Exception e) {
                    log.warn("[Scheduler] 근무일 생성 스킵. crewId={} date={}",
                        schedule.getCrewId(), workDate);
                }
            });
        });

        log.info("[Scheduler] 주간 근무일 자동 생성 완료");
    }
}
