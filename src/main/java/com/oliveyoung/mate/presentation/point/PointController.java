package com.oliveyoung.mate.presentation.point;

import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.point.command.CancelUseCommand;
import com.oliveyoung.mate.application.point.command.GrantPointManualCommand;
import com.oliveyoung.mate.application.point.command.InitPointCommand;
import com.oliveyoung.mate.application.point.command.UsePointCommand;
import com.oliveyoung.mate.application.point.result.LedgerHistoryResult;
import com.oliveyoung.mate.application.point.result.PointBalanceResult;
import com.oliveyoung.mate.application.point.result.UsePointResult;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    // 잔액 조회
    @GetMapping("/balance/{crewId}")
    public ResponseEntity<PointBalanceResult> getBalance(@PathVariable UUID crewId) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        return ResponseEntity.ok(pointService.getBalance(crewId));
    }

    // 포인트 사용
    @PostMapping("/use/{crewId}")
    public ResponseEntity<UsePointResult> use(
            @PathVariable UUID crewId,
            @Valid @RequestBody UsePointRequest request) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        UsePointResult result = pointService.use(new UsePointCommand(
            crewId,
            request.amount(),
            request.description(),
            request.usedAt()
        ));
        return ResponseEntity.ok(result);
    }

    // 내역 조회
    @GetMapping("/history/{crewId}")
    public ResponseEntity<List<LedgerHistoryResult>> getHistory(@PathVariable UUID crewId) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        return ResponseEntity.ok(pointService.getLedgerHistory(crewId));
    }

    // 초기 포인트 등록 (최초 1회)
    @PostMapping("/initialize/{crewId}")
    public ResponseEntity<String> initialize(
            @PathVariable UUID crewId,
            @Valid @RequestBody InitPointRequest request) {
        SecurityUtils.validateSelfOrAdmin(crewId);
        pointService.initialize(new InitPointCommand(crewId, request.amount()));
        return ResponseEntity.ok("초기 포인트 등록 완료!");
    }

    // 포인트 사용 취소 (당일 건)
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelUse(@Valid @RequestBody CancelUseRequest request) {
        SecurityUtils.validateSelfOrAdmin(request.crewId());
        pointService.cancelUse(new CancelUseCommand(request.ledgerId(), request.crewId()));
        return ResponseEntity.ok("포인트 사용이 취소됐습니다.");
    }

    // 소급 적립 (관리자 전용)
    @PostMapping("/grant/manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> grantManual(@Valid @RequestBody GrantPointManualRequest request) {
        pointService.grantPointForDate(
            new GrantPointManualCommand(request.crewId(), request.workDate()));
        return ResponseEntity.ok("포인트 소급 적립 완료!");
    }
}

