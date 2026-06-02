package com.oliveyoung.mate.application.schedule;

import com.oliveyoung.mate.application.schedule.command.SaveScheduleCommand;
import com.oliveyoung.mate.application.schedule.result.ScheduleResult;
import com.oliveyoung.mate.domain.schedule.model.CrewSchedule;
import com.oliveyoung.mate.domain.schedule.repository.CrewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CrewScheduleRepository scheduleRepository;

    // ── 근무요일 저장 (기존 설정 비활성화 후 신규 등록) ──
    @Transactional
    public void saveSchedule(SaveScheduleCommand cmd) {
        // 기존 설정 비활성화
        scheduleRepository.deactivateByCrewId(cmd.crewId());

        // 신규 등록
        CrewSchedule schedule = CrewSchedule.create(
            cmd.crewId(),
            cmd.daysOfWeek(),
            cmd.startDate(),
            cmd.endDate()
        );
        scheduleRepository.save(schedule);
    }

    // ── 내 근무요일 조회 ───────────────────────────────
    @Transactional(readOnly = true)
    public Optional<ScheduleResult> getMySchedule(java.util.UUID crewId) {
        return scheduleRepository.findActiveByCrewId(crewId)
            .map(s -> new ScheduleResult(s.getDaysOfWeek(), s.getStartDate(), s.getEndDate()));
    }
}