package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.attendance.AttendanceService;
import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.point.command.GrantPointManualCommand;
import com.oliveyoung.mate.application.point.command.InitPointCommand;
import com.oliveyoung.mate.application.point.result.LedgerHistoryResult;
import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.crew.model.Crew;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Controller
@RequestMapping("/admin/crews/{crewId}")
@RequiredArgsConstructor
public class AdminCrewPageController {

    private final CrewRepository crewRepository;
    private final WorkDayRepository workDayRepository;
    private final AttendanceService attendanceService;
    private final PointService pointService;

    @GetMapping
    public String detail(
            @PathVariable UUID crewId,
            @RequestParam(defaultValue = "workdays") String tab,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {
        Crew crew = crewRepository.findById(crewId).orElseThrow();
        model.addAttribute("crew", crew);
        model.addAttribute("activeTab", tab);

        LocalDate base = (year != null && month != null)
            ? LocalDate.of(year, month, 1)
            : LocalDate.now(ZoneId.of("Asia/Seoul")).withDayOfMonth(1);
        model.addAttribute("year", base.getYear());
        model.addAttribute("month", base.getMonthValue());

        switch (tab) {
            case "workdays" -> model.addAttribute("workdays",
                workDayRepository.findByCrewIdAndWorkDateBetween(
                        crewId, base, base.withDayOfMonth(base.lengthOfMonth()))
                    .stream().map(AdminCrewPageController::toWorkDayView).toList());
            case "history" -> {
                try {
                    model.addAttribute("balance", pointService.getBalance(crewId));
                    model.addAttribute("ledgers", pointService.getLedgerHistory(crewId).stream()
                        .map(AdminCrewPageController::toLedgerView).toList());
                } catch (PointAccountNotFoundException e) {
                    model.addAttribute("noPointAccount", true);
                }
            }
            default -> { /* grant/info: crew 모델 속성만으로 렌더 */ }
        }
        return "admin-crew-detail";
    }

    @PostMapping("/workdays/cancel")
    public String cancelWorkDay(
            @PathVariable UUID crewId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam int year, @RequestParam int month) {
        attendanceService.cancelWorkDay(crewId, workDate);
        return redirectToWorkdays(crewId, year, month);
    }

    @PostMapping("/workdays/reinstate")
    public String reinstateWorkDay(
            @PathVariable UUID crewId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam int year, @RequestParam int month) {
        attendanceService.reinstateWorkDay(crewId, workDate);
        return redirectToWorkdays(crewId, year, month);
    }

    @PostMapping("/points/initialize")
    public String initializePoints(@PathVariable UUID crewId, @RequestParam long amount, Model model) {
        try {
            pointService.initialize(new InitPointCommand(crewId, amount));
        } catch (IllegalStateException e) {
            return renderGrantError(crewId, e.getMessage(), model);
        }
        return "redirect:/admin/crews/" + crewId + "?tab=grant";
    }

    @PostMapping("/points/grant-manual")
    public String grantManual(
            @PathVariable UUID crewId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            Model model) {
        try {
            pointService.grantPointForDate(new GrantPointManualCommand(crewId, workDate));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return renderGrantError(crewId, e.getMessage(), model);
        }
        return "redirect:/admin/crews/" + crewId + "?tab=grant";
    }

    private String renderGrantError(UUID crewId, String message, Model model) {
        model.addAttribute("crew", crewRepository.findById(crewId).orElseThrow());
        model.addAttribute("activeTab", "grant");
        model.addAttribute("grantError", message);
        return "admin-crew-detail";
    }

    private String redirectToWorkdays(UUID crewId, int year, int month) {
        return "redirect:/admin/crews/" + crewId + "?tab=workdays&year=" + year + "&month=" + month;
    }

    private record WorkDayView(LocalDate workDate, String statusLabel, String statusColor, String statusBg, boolean skipped) {}

    private static WorkDayView toWorkDayView(WorkDay w) {
        if (w.isSkipped()) {
            return new WorkDayView(w.getWorkDate(), "결근", "#E53935", "#FFF5F5", true);
        }
        if (w.isPointGranted()) {
            return new WorkDayView(w.getWorkDate(), "적립 완료", "#1B9E5B", "#F0FFF4", false);
        }
        return new WorkDayView(w.getWorkDate(), "대기중", "#E65100", "#FFFBF0", false);
    }

    private record LedgerView(String label, String color, String sign, long amount, String description, java.time.LocalDateTime createdAt) {}

    private static LedgerView toLedgerView(LedgerHistoryResult r) {
        String label, color, sign;
        switch (r.ledgerType()) {
            case EARN -> { label = "적립"; color = "#1B9E5B"; sign = "+"; }
            case USE -> { label = "사용"; color = "#E53935"; sign = "-"; }
            case INIT -> { label = "초기 지급"; color = "#1565C0"; sign = "+"; }
            default -> { label = "소멸"; color = "#888888"; sign = "-"; } // EXPIRE
        }
        return new LedgerView(label, color, sign, r.amount(), r.description(), r.createdAt());
    }
}
