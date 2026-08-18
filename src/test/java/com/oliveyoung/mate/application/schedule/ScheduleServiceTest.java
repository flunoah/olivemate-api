package com.oliveyoung.mate.application.schedule;

import com.oliveyoung.mate.application.attendance.AttendanceService;
import com.oliveyoung.mate.application.attendance.command.RegisterWorkDayCommand;
import com.oliveyoung.mate.application.schedule.command.SaveScheduleCommand;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.schedule.model.CrewSchedule;
import com.oliveyoung.mate.domain.schedule.repository.CrewScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    @DisplayName("다음 주 근무일 생성 시 등록된 요일만 등록되고 나머지 요일은 등록되지 않는다")
    void only_registered_days_of_week_are_generated() {
        UUID crewId = UUID.randomUUID();
        CrewSchedule schedule = CrewSchedule.create(
            crewId, List.of(1, 3, 5), // 월, 수, 금
            LocalDate.now().minusDays(30), null);
        when(scheduleRepository.findAllActive()).thenReturn(List.of(schedule));

        scheduleService.generateNextWeekWorkDays();

        LocalDate nextMonday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);
        verify(attendanceService, times(3)).registerWorkDay(any());
        verify(attendanceService).registerWorkDay(
            new RegisterWorkDayCommand(crewId, nextMonday));
        verify(attendanceService).registerWorkDay(
            new RegisterWorkDayCommand(crewId, nextMonday.plusDays(2))); // 수
        verify(attendanceService).registerWorkDay(
            new RegisterWorkDayCommand(crewId, nextMonday.plusDays(4))); // 금
        verify(attendanceService, never()).registerWorkDay(
            eq(new RegisterWorkDayCommand(crewId, nextMonday.plusDays(1)))); // 화
        verify(attendanceService, never()).registerWorkDay(
            eq(new RegisterWorkDayCommand(crewId, nextMonday.plusDays(5)))); // 토
        verify(attendanceService, never()).registerWorkDay(
            eq(new RegisterWorkDayCommand(crewId, nextMonday.plusDays(6)))); // 일
    }
}
