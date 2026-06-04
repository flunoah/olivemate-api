package com.oliveyoung.mate.application.attendance;

import com.oliveyoung.mate.application.attendance.command.RegisterWorkDayCommand;
import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final WorkDayRepository        workDayRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerWorkDay(RegisterWorkDayCommand cmd) {

        // 중복 등록 방지
        if (workDayRepository.existsByCrewIdAndWorkDate(
                cmd.crewId(), cmd.workDate())) {
            throw new IllegalStateException(
                "이미 등록된 근무일입니다. date=" + cmd.workDate()
            );
        }

        // 근무일 등록 → 내부에서 WorkDayRegisteredEvent 발행
        WorkDay workDay = WorkDay.register(cmd.crewId(), cmd.workDate());
        workDayRepository.save(workDay);

        // 도메인 이벤트 발행 → PointService.earn() 자동 호출
        workDay.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

    // ── 결근·조퇴 처리 ────────────────────────────
    @Transactional
    public void cancelWorkDay(UUID crewId, LocalDate date) {
        workDayRepository.findByCrewIdAndWorkDate(crewId, date)
            .ifPresentOrElse(
                workDay -> {
                    workDay.skip();
                    workDayRepository.save(workDay);
                },
                // 아직 스케줄러가 생성하기 전 → 결근 레코드 선점
                () -> workDayRepository.save(WorkDay.createAbsent(crewId, date))
            );
    }

    // ── 결근 복원 ──────────────────────────────────
    @Transactional
    public void reinstateWorkDay(UUID crewId, LocalDate date) {
        workDayRepository.findByCrewIdAndWorkDate(crewId, date)
            .ifPresentOrElse(
                workDay -> {
                    if (!workDay.isSkipped()) {
                        throw new IllegalStateException("이미 정상 근무일입니다. date=" + date);
                    }
                    workDay.reinstate();
                    workDayRepository.save(workDay);
                },
                () -> registerWorkDay(new RegisterWorkDayCommand(crewId, date))
            );
    }

    // ── 이번 주 근무일 조회 ────────────────────────
    @Transactional(readOnly = true)
    public List<String> getThisWeekWorkDays(UUID crewId) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        return workDayRepository.findByCrewIdAndWorkDateBetween(crewId, monday, sunday)
            .stream()
            .filter(w -> !w.isSkipped())
            .map(w -> w.getWorkDate().toString())
            .toList();
    }
}