package com.oliveyoung.mate.application.schedule;

import com.oliveyoung.mate.application.attendance.AttendanceService;
import com.oliveyoung.mate.application.attendance.command.RegisterWorkDayCommand;
import com.oliveyoung.mate.application.schedule.command.SaveScheduleCommand;
import com.oliveyoung.mate.application.schedule.result.ScheduleResult;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.schedule.model.CrewSchedule;
import com.oliveyoung.mate.domain.schedule.repository.CrewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CrewScheduleRepository scheduleRepository;
    private final AttendanceService      attendanceService;
    private final WorkDayRepository      workDayRepository;

    // ── 근무요일 저장 ──────────────────────────────
    @Transactional
    public void saveSchedule(SaveScheduleCommand cmd) {
        scheduleRepository.deactivateByCrewId(cmd.crewId());
        scheduleRepository.save(CrewSchedule.create(
            cmd.crewId(),
            cmd.daysOfWeek(),
            cmd.startDate(),
            cmd.endDate()
        ));
    }

    // ── 내 근무요일 조회 ───────────────────────────
    @Transactional(readOnly = true)
    public Optional<ScheduleResult> getMySchedule(UUID crewId) {
        return scheduleRepository.findActiveByCrewId(crewId)
            .map(s -> new ScheduleResult(s.getDaysOfWeek(), s.getStartDate(), s.getEndDate()));
    }

    // ── Cron/Admin 주간 근무일 생성 ────────────────
    public void generateNextWeekWorkDays() {
        log.info("[Admin Cron] 주간 근무일 생성 시작 {}", LocalDate.now());
        int[] count = {0};

        LocalDate monday = LocalDate.now(ZoneId.of("Asia/Seoul")).with(DayOfWeek.MONDAY).plusWeeks(1);

        scheduleRepository.findAllActive().forEach(schedule -> {
            if (schedule.getStartDate().isAfter(monday)) return;

            schedule.getDaysOfWeek().forEach(dayOfWeek -> {
                LocalDate workDate = monday.plusDays(
                    dayOfWeek == 0 ? 6 : dayOfWeek - 1
                );

                if (schedule.getEndDate() != null && workDate.isAfter(schedule.getEndDate())) {
                    log.info("[Admin Cron] 스케줄 종료일 초과 스킵. crewId={} date={}",
                        schedule.getCrewId(), workDate);
                    return;
                }

                boolean isAbsent = workDayRepository
                    .findByCrewIdAndWorkDate(schedule.getCrewId(), workDate)
                    .map(w -> w.isSkipped())
                    .orElse(false);

                if (isAbsent) {
                    log.info("[Admin Cron] 결근 처리 날짜 스킵. crewId={} date={}",
                        schedule.getCrewId(), workDate);
                    return;
                }

                try {
                    attendanceService.registerWorkDay(
                        new RegisterWorkDayCommand(schedule.getCrewId(), workDate)
                    );
                    count[0]++;
                } catch (Exception e) {
                    log.warn("[Admin Cron] 근무일 생성 스킵. crewId={} date={}",
                        schedule.getCrewId(), workDate);
                }
            });
        });

        log.info("[Admin Cron] 처리 완료 {}건", count[0]);
    }
}
