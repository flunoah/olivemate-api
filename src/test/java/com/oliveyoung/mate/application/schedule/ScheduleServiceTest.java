package com.oliveyoung.mate.application.schedule;

import com.oliveyoung.mate.application.attendance.AttendanceService;
import com.oliveyoung.mate.application.attendance.command.RegisterWorkDayCommand;
import com.oliveyoung.mate.application.schedule.command.SaveScheduleCommand;
import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.schedule.model.CrewSchedule;
import com.oliveyoung.mate.domain.schedule.repository.CrewScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private CrewScheduleRepository scheduleRepository;
    private AttendanceService      attendanceService;
    private WorkDayRepository      workDayRepository;
    private ScheduleService        scheduleService;

    private static final LocalDate TODAY = LocalDate.now();
    private static final int       TODAY_CODE = TODAY.getDayOfWeek().getValue() % 7; // Mon=1..Sat=6, Sun=0
    private static final int       OTHER_CODE = (TODAY_CODE + 1) % 7; // 오늘과 항상 다른 요일

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(CrewScheduleRepository.class);
        attendanceService  = mock(AttendanceService.class);
        workDayRepository  = mock(WorkDayRepository.class);

        when(workDayRepository.findByCrewIdAndWorkDate(any(), any())).thenReturn(Optional.empty());

        scheduleService = new ScheduleService(scheduleRepository, attendanceService, workDayRepository);
    }

    @Test
    @DisplayName("처음 저장하면 새 활성 스케줄이 생성된다")
    void first_save_creates_active_schedule() {
        UUID crewId = UUID.randomUUID();
        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(1, 3, 5), LocalDate.of(2026, 1, 1), null);

        scheduleService.saveSchedule(cmd);

        verify(scheduleRepository).deactivateByCrewId(crewId);
        var captor = org.mockito.ArgumentCaptor.forClass(CrewSchedule.class);
        verify(scheduleRepository).save(captor.capture());

        CrewSchedule saved = captor.getValue();
        assertThat(saved.getCrewId()).isEqualTo(crewId);
        assertThat(saved.getDaysOfWeek()).containsExactly(1, 3, 5);
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(saved.getEndDate()).isNull();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("요일을 바꿔 저장하면 기존 스케줄은 비활성화되고 새 요일 전체로 교체된다")
    void changing_days_replaces_the_whole_set() {
        UUID crewId = UUID.randomUUID();
        SaveScheduleCommand newSchedule = new SaveScheduleCommand(
            crewId, List.of(2, 4), LocalDate.of(2026, 2, 1), null);

        scheduleService.saveSchedule(newSchedule);

        verify(scheduleRepository, times(1)).deactivateByCrewId(crewId);
        var captor = org.mockito.ArgumentCaptor.forClass(CrewSchedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getDaysOfWeek()).containsExactly(2, 4);
    }

    @Test
    @DisplayName("비활성화가 저장보다 먼저 일어난다")
    void deactivate_happens_before_save() {
        UUID crewId = UUID.randomUUID();
        scheduleService.saveSchedule(new SaveScheduleCommand(
            crewId, List.of(1), LocalDate.of(2026, 1, 1), null));

        var order = inOrder(scheduleRepository);
        order.verify(scheduleRepository).deactivateByCrewId(crewId);
        order.verify(scheduleRepository).save(any(CrewSchedule.class));
    }

    @Test
    @DisplayName("저장은 해당 크루에만 영향을 준다")
    void save_only_affects_the_target_crew() {
        UUID crewA = UUID.randomUUID();
        UUID crewB = UUID.randomUUID();

        scheduleService.saveSchedule(new SaveScheduleCommand(
            crewA, List.of(1, 2), LocalDate.of(2026, 1, 1), null));

        verify(scheduleRepository).deactivateByCrewId(crewA);
        verify(scheduleRepository, never()).deactivateByCrewId(crewB);
    }

    // ── generateTodayWorkDays (일일 배치) ──────────────────────

    @Test
    @DisplayName("오늘이 스케줄 요일과 맞으면 오늘 근무일이 생성된다")
    void generateTodayWorkDays_registers_when_today_matches_schedule() {
        UUID crewId = UUID.randomUUID();
        CrewSchedule schedule = CrewSchedule.create(
            crewId, List.of(TODAY_CODE), TODAY.minusDays(30), null);
        when(scheduleRepository.findAllEffectiveOn(TODAY)).thenReturn(List.of(schedule));

        scheduleService.generateTodayWorkDays();

        verify(attendanceService).registerWorkDay(new RegisterWorkDayCommand(crewId, TODAY));
    }

    @Test
    @DisplayName("오늘이 스케줄 요일에 없으면 생성되지 않는다")
    void generateTodayWorkDays_skips_when_today_not_in_schedule() {
        UUID crewId = UUID.randomUUID();
        CrewSchedule schedule = CrewSchedule.create(
            crewId, List.of(OTHER_CODE), TODAY.minusDays(30), null);
        when(scheduleRepository.findAllEffectiveOn(TODAY)).thenReturn(List.of(schedule));

        scheduleService.generateTodayWorkDays();

        verify(attendanceService, never()).registerWorkDay(any());
    }

    @Test
    @DisplayName("시작일이 내일이면 오늘은 생성되지 않는다")
    void generateTodayWorkDays_skips_before_start_date() {
        UUID crewId = UUID.randomUUID();
        CrewSchedule schedule = CrewSchedule.create(
            crewId, List.of(TODAY_CODE), TODAY.plusDays(1), null);
        when(scheduleRepository.findAllEffectiveOn(TODAY)).thenReturn(List.of(schedule));

        scheduleService.generateTodayWorkDays();

        verify(attendanceService, never()).registerWorkDay(any());
    }

    @Test
    @DisplayName("종료일이 어제면 오늘은 생성되지 않는다")
    void generateTodayWorkDays_skips_after_end_date() {
        UUID crewId = UUID.randomUUID();
        CrewSchedule schedule = CrewSchedule.create(
            crewId, List.of(TODAY_CODE), TODAY.minusDays(30), TODAY.minusDays(1));
        when(scheduleRepository.findAllEffectiveOn(TODAY)).thenReturn(List.of(schedule));

        scheduleService.generateTodayWorkDays();

        verify(attendanceService, never()).registerWorkDay(any());
    }

    // ── saveSchedule의 오늘 즉시 동기화 ─────────────────────────

    @Test
    @DisplayName("새 스케줄이 오늘 요일을 포함하면 저장 즉시 오늘 근무일이 생성된다")
    void saveSchedule_immediately_registers_today_when_new_schedule_matches() {
        UUID crewId = UUID.randomUUID();
        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(TODAY_CODE), TODAY.minusDays(30), null);

        scheduleService.saveSchedule(cmd);

        verify(attendanceService).registerWorkDay(new RegisterWorkDayCommand(crewId, TODAY));
    }

    @Test
    @DisplayName("새 스케줄에 오늘 요일이 없고 기존 근무일이 있으면 취소된다")
    void saveSchedule_cancels_today_when_new_schedule_no_longer_matches() {
        UUID crewId = UUID.randomUUID();
        WorkDay existing = WorkDay.register(crewId, TODAY);
        when(workDayRepository.findByCrewIdAndWorkDate(crewId, TODAY)).thenReturn(Optional.of(existing));

        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(OTHER_CODE), TODAY.minusDays(30), null);

        scheduleService.saveSchedule(cmd);

        verify(attendanceService).cancelWorkDay(crewId, TODAY);
        verify(attendanceService, never()).registerWorkDay(any());
    }

    @Test
    @DisplayName("이미 결근 처리된 오늘 근무일은 다시 취소하지 않는다")
    void saveSchedule_does_not_recancel_already_skipped_today() {
        UUID crewId = UUID.randomUUID();
        WorkDay absentDay = WorkDay.createAbsent(crewId, TODAY);
        when(workDayRepository.findByCrewIdAndWorkDate(crewId, TODAY)).thenReturn(Optional.of(absentDay));

        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(OTHER_CODE), TODAY.minusDays(30), null);

        scheduleService.saveSchedule(cmd);

        verify(attendanceService, never()).cancelWorkDay(any(), any());
    }

    @Test
    @DisplayName("오늘 요일이 안 맞고 기존 근무일도 없으면 아무 것도 하지 않는다")
    void saveSchedule_does_nothing_when_today_not_matched_and_no_existing_workday() {
        UUID crewId = UUID.randomUUID();
        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(OTHER_CODE), TODAY.minusDays(30), null);

        scheduleService.saveSchedule(cmd);

        verify(attendanceService, never()).registerWorkDay(any());
        verify(attendanceService, never()).cancelWorkDay(any(), any());
    }

    @Test
    @DisplayName("오늘이 이미 결근 처리돼 있으면 요일이 맞아도 재등록하지 않는다")
    void saveSchedule_skips_registration_when_today_already_marked_absent() {
        UUID crewId = UUID.randomUUID();
        WorkDay absentDay = WorkDay.createAbsent(crewId, TODAY);
        when(workDayRepository.findByCrewIdAndWorkDate(crewId, TODAY)).thenReturn(Optional.of(absentDay));

        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(TODAY_CODE), TODAY.minusDays(30), null);

        scheduleService.saveSchedule(cmd);

        verify(attendanceService, never()).registerWorkDay(any());
    }

    @Test
    @DisplayName("시작일이 미래면 오늘 요일이 안 맞아도 기존 오늘 근무일을 취소하지 않는다")
    void saveSchedule_with_future_start_date_does_not_cancel_today() {
        UUID crewId = UUID.randomUUID();
        WorkDay existing = WorkDay.register(crewId, TODAY);
        when(workDayRepository.findByCrewIdAndWorkDate(crewId, TODAY)).thenReturn(Optional.of(existing));

        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(OTHER_CODE), TODAY.plusDays(1), null);

        scheduleService.saveSchedule(cmd);

        verify(attendanceService, never()).cancelWorkDay(any(), any());
    }

    @Test
    @DisplayName("시작일이 미래면 오늘 요일이 맞아도 오늘 근무일을 생성하지 않는다")
    void saveSchedule_with_future_start_date_does_not_register_today() {
        UUID crewId = UUID.randomUUID();
        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(TODAY_CODE), TODAY.plusDays(1), null);

        scheduleService.saveSchedule(cmd);

        verify(attendanceService, never()).registerWorkDay(any());
    }

    @Test
    @DisplayName("오늘 등록 중 중복 등록 예외가 나도 스케줄 저장은 실패하지 않는다")
    void saveSchedule_swallows_duplicate_registration_and_does_not_fail() {
        UUID crewId = UUID.randomUUID();
        doThrow(new IllegalStateException("이미 등록된 근무일입니다. date=" + TODAY))
            .when(attendanceService)
            .registerWorkDay(eq(new RegisterWorkDayCommand(crewId, TODAY)));

        SaveScheduleCommand cmd = new SaveScheduleCommand(
            crewId, List.of(TODAY_CODE), TODAY.minusDays(30), null);

        assertThatCode(() -> scheduleService.saveSchedule(cmd)).doesNotThrowAnyException();
        verify(scheduleRepository).save(any(CrewSchedule.class));
    }
}
