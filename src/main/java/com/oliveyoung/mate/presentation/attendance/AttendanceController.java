package com.oliveyoung.mate.presentation.attendance;

import com.oliveyoung.mate.application.attendance.AttendanceService;
import com.oliveyoung.mate.application.attendance.command.RegisterWorkDayCommand;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterWorkDayRequest request) {
        SecurityUtils.validateSelfOrAdmin(request.crewId());
        attendanceService.registerWorkDay(new RegisterWorkDayCommand(
            request.crewId(),
            request.workDate()
        ));
        return ResponseEntity.ok("근무일 등록 완료. 포인트는 내일 지급됩니다.");
    }

    @GetMapping("/week/{crewId}")
    public ResponseEntity<List<String>> getThisWeekWorkDays(@PathVariable UUID crewId) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        return ResponseEntity.ok(attendanceService.getThisWeekWorkDays(crewId));
    }

    @PutMapping("/reinstate")
    public ResponseEntity<String> reinstateWorkDay(
            @RequestParam UUID crewId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        attendanceService.reinstateWorkDay(crewId, workDate);
        return ResponseEntity.ok("근무일 복원 완료.");
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelWorkDay(
            @RequestParam UUID crewId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        UUID callerId = SecurityUtils.authenticatedCrewId();
        log.warn("[Attendance] 결근 처리 요청. caller={} target={} date={}", callerId, crewId, workDate);
        attendanceService.cancelWorkDay(crewId, workDate);
        return ResponseEntity.ok("결근 처리됐어요.");
    }
}
