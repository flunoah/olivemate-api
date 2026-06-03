package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.schedule.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    @Value("${admin.secret.key}")
    private String adminSecretKey;

    private final PointService    pointService;
    private final ScheduleService scheduleService;

    private boolean isUnauthorized(String key) {
        return !adminSecretKey.equals(key);
    }

    @PostMapping("/grant-points-all")
    public ResponseEntity<String> grantPointsAll(
            @RequestHeader("X-Admin-Key") String adminKey) {
        if (isUnauthorized(adminKey)) return ResponseEntity.status(403).body("Forbidden");
        pointService.grantPointsForAll();
        return ResponseEntity.ok("포인트 적립 완료");
    }

    @PostMapping("/expire-points-all")
    public ResponseEntity<String> expirePointsAll(
            @RequestHeader("X-Admin-Key") String adminKey) {
        if (isUnauthorized(adminKey)) return ResponseEntity.status(403).body("Forbidden");
        pointService.expireAllPoints();
        return ResponseEntity.ok("포인트 만료 처리 완료");
    }

    @PostMapping("/generate-workdays")
    public ResponseEntity<String> generateWorkdays(
            @RequestHeader("X-Admin-Key") String adminKey) {
        if (isUnauthorized(adminKey)) return ResponseEntity.status(403).body("Forbidden");
        scheduleService.generateNextWeekWorkDays();
        return ResponseEntity.ok("근무일 생성 완료");
    }
}
