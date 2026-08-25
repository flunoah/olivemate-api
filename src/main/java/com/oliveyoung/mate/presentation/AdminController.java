package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.product.ProductSyncService;
import com.oliveyoung.mate.application.product.ProductUploadItem;
import com.oliveyoung.mate.application.product.result.ProductUploadResult;
import com.oliveyoung.mate.application.schedule.ScheduleService;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.crew.model.Crew;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import com.oliveyoung.mate.infrastructure.product.excel.ProductExcelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    @Value("${admin.secret.key}")
    private String adminSecretKey;

    private final PointService    pointService;
    private final ScheduleService scheduleService;
    private final CrewRepository  crewRepository;
    private final WorkDayRepository workDayRepository;
    private final ProductExcelParser productExcelParser;
    private final ProductSyncService productSyncService;

    record CrewSummary(UUID crewId, String name, String loginId, String role) {}
    record WorkDaySummary(LocalDate workDate, boolean pointGranted, boolean skipped) {}

    private boolean isUnauthorized(String key) {
        return !adminSecretKey.equals(key);
    }

    @PostMapping("/grant-points-all")
    public ResponseEntity<String> grantPointsAll(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (isUnauthorized(adminKey)) return ResponseEntity.status(403).body("Forbidden");
        pointService.grantPointsForAll();
        return ResponseEntity.ok("포인트 적립 완료");
    }

    @PostMapping("/expire-points-all")
    public ResponseEntity<String> expirePointsAll(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (isUnauthorized(adminKey)) return ResponseEntity.status(403).body("Forbidden");
        pointService.expireAllPoints();
        return ResponseEntity.ok("포인트 만료 처리 완료");
    }

    @PostMapping("/generate-workdays")
    public ResponseEntity<String> generateWorkdays(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (isUnauthorized(adminKey)) return ResponseEntity.status(403).body("Forbidden");
        scheduleService.generateTodayWorkDays();
        return ResponseEntity.ok("근무일 생성 완료");
    }

    // ── 어드민 관리 UI용 ───────────────────────────

    @GetMapping("/crews")
    public ResponseEntity<List<CrewSummary>> getCrews() {
        SecurityUtils.validateAdmin();
        List<CrewSummary> result = crewRepository.findAllActiveByRole(Crew.Role.CREW).stream()
            .map(c -> new CrewSummary(c.getCrewId(), c.getName(), c.getLoginId(), c.getRole().name()))
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/workdays")
    public ResponseEntity<List<WorkDaySummary>> getWorkDays(
            @RequestParam UUID crewId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        SecurityUtils.validateAdmin();
        List<WorkDaySummary> result = workDayRepository
            .findByCrewIdAndWorkDateBetween(crewId, from, to).stream()
            .map(w -> new WorkDaySummary(w.getWorkDate(), w.isPointGranted(), w.isSkipped()))
            .toList();
        return ResponseEntity.ok(result);
    }

    // 상품 카탈로그 엑셀 업로드 (어드민 UI용, 크롤링은 앱 바깥에서 이뤄짐)
    @PostMapping("/products/upload")
    public ResponseEntity<ProductUploadResult> uploadProducts(@RequestParam("file") MultipartFile file) {
        SecurityUtils.validateAdmin();
        List<ProductUploadItem> items = productExcelParser.parse(file);
        int synced = productSyncService.syncAll(items);
        return ResponseEntity.ok(new ProductUploadResult(items.size(), synced, items.size() - synced));
    }
}
