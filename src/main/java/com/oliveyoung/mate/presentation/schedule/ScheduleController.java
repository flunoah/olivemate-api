package com.oliveyoung.mate.presentation.schedule;

import com.oliveyoung.mate.application.schedule.ScheduleService;
import com.oliveyoung.mate.application.schedule.command.SaveScheduleCommand;
import com.oliveyoung.mate.application.schedule.result.ScheduleResult;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 근무요일 저장
    @PostMapping
    public ResponseEntity<String> saveSchedule(@Valid @RequestBody SaveScheduleRequest request) {
        SecurityUtils.validateSelfOrAdmin(request.crewId());
        scheduleService.saveSchedule(new SaveScheduleCommand(
            request.crewId(),
            request.daysOfWeek(),
            request.startDate(),
            request.endDate()
        ));
        return ResponseEntity.ok("근무 요일이 저장됐어요!");
    }

    // 내 근무요일 조회
    @GetMapping("/me/{crewId}")
    public ResponseEntity<ScheduleResult> getMySchedule(@PathVariable UUID crewId) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        return scheduleService.getMySchedule(crewId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}