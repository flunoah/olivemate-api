package com.oliveyoung.mate.application.schedule;

import com.oliveyoung.mate.application.JobReport;
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
        CrewSchedule schedule = CrewSchedule.create(
            cmd.crewId(),
            cmd.daysOfWeek(),
            cmd.startDate(),
            cmd.endDate()
        );
        scheduleRepository.save(schedule);
        syncToday(schedule);
    }

    // ── 스케줄 변경분을 오늘 날짜에 즉시 반영 ──────
    // 내일 이후는 DailyWorkDayScheduler가 그날 활성 스케줄을 보고 처리하므로, 여기서는 오늘만 동기화한다.
    private void syncToday(CrewSchedule schedule) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (today.isBefore(schedule.getStartDate())) {
            return; // 아직 시작 전 — 오늘은 findAllEffectiveOn()이 이전 스케줄로 계속 처리한다
        }

        int[] count = {0, 0, 0};
        if (matchesSchedule(schedule, today)) {
            tryRegisterWorkDay(schedule, today, count);
        } else {
            cancelIfExists(schedule.getCrewId(), today);
        }
    }

    private void cancelIfExists(UUID crewId, LocalDate date) {
        workDayRepository.findByCrewIdAndWorkDate(crewId, date)
            .filter(w -> !w.isSkipped())
            .ifPresent(w -> {
                attendanceService.cancelWorkDay(crewId, date);
                log.info("[스케줄 변경] 새 스케줄에 없는 오늘 근무일 취소. crewId={} date={}", crewId, date);
            });
    }

    // ── 내 근무요일 조회 ───────────────────────────
    @Transactional(readOnly = true)
    public Optional<ScheduleResult> getMySchedule(UUID crewId) {
        return scheduleRepository.findActiveByCrewId(crewId)
            .map(s -> new ScheduleResult(s.getDaysOfWeek(), s.getStartDate(), s.getEndDate()));
    }

    // ── Cron/Admin 일일 근무일 생성 ─────────────────
    public JobReport generateTodayWorkDays() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("[Daily Cron] 일일 근무일 생성 시작 {}", today);
        int[] count = {0, 0, 0}; // success, skipped, failed

        scheduleRepository.findAllEffectiveOn(today).forEach(schedule -> {
            if (matchesSchedule(schedule, today)) {
                tryRegisterWorkDay(schedule, today, count);
            }
        });

        log.info("[Daily Cron] 처리 완료 {}건 (스킵 {}건, 실패 {}건)", count[0], count[1], count[2]);
        return new JobReport("일일 근무일 생성", today, count[0], count[1], count[2]);
    }

    // ── 해당 날짜가 스케줄의 요일 패턴에 포함되는지 ─
    private boolean matchesSchedule(CrewSchedule schedule, LocalDate date) {
        LocalDate monday = date.with(DayOfWeek.MONDAY);
        return schedule.getDaysOfWeek().stream()
            .anyMatch(dow -> resolveWorkDate(monday, dow).equals(date));
    }

    // ── 요일 코드(0=일~6=토) → 해당 주의 실제 날짜 ─
    private static LocalDate resolveWorkDate(LocalDate weekMonday, int dayOfWeek) {
        return weekMonday.plusDays(dayOfWeek == 0 ? 6 : dayOfWeek - 1);
    }

    // ── 날짜 1건에 대한 근무일 생성 시도 (시작일/종료일/결근/중복 스킵) ─
    private void tryRegisterWorkDay(CrewSchedule schedule, LocalDate workDate, int[] count) {
        if (workDate.isBefore(schedule.getStartDate())) {
            log.info("[근무일 생성] 스케줄 시작일 이전 스킵. crewId={} date={}",
                schedule.getCrewId(), workDate);
            count[1]++;
            return;
        }

        if (schedule.getEndDate() != null && workDate.isAfter(schedule.getEndDate())) {
            log.info("[근무일 생성] 스케줄 종료일 초과 스킵. crewId={} date={}",
                schedule.getCrewId(), workDate);
            count[1]++;
            return;
        }

        boolean isAbsent = workDayRepository
            .findByCrewIdAndWorkDate(schedule.getCrewId(), workDate)
            .map(w -> w.isSkipped())
            .orElse(false);

        if (isAbsent) {
            log.info("[근무일 생성] 결근 처리 날짜 스킵. crewId={} date={}",
                schedule.getCrewId(), workDate);
            count[1]++;
            return;
        }

        try {
            attendanceService.registerWorkDay(
                new RegisterWorkDayCommand(schedule.getCrewId(), workDate)
            );
            count[0]++;
        } catch (IllegalStateException e) {
            // 이미 등록된 근무일 — 재실행 시 정상 발생하므로 실패가 아님
            log.info("[근무일 생성] 근무일 중복 스킵. crewId={} date={}",
                schedule.getCrewId(), workDate);
            count[1]++;
        } catch (Exception e) {
            count[2]++;
            log.error("[근무일 생성] 근무일 생성 실패. crewId={} date={}",
                schedule.getCrewId(), workDate, e);
        }
    }
}
