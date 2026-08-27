package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.attendance.AttendanceService;
import com.oliveyoung.mate.application.attendance.command.RegisterWorkDayCommand;
import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.point.command.CancelUseCommand;
import com.oliveyoung.mate.application.point.command.UsePointCommand;
import com.oliveyoung.mate.application.point.result.PointBalanceResult;
import com.oliveyoung.mate.application.point.result.PointUsePreviewResult;
import com.oliveyoung.mate.application.point.result.UsePointResult;
import com.oliveyoung.mate.application.schedule.ScheduleService;
import com.oliveyoung.mate.application.schedule.result.ScheduleResult;
import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.point.InsufficientPointException;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardPageController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String[] DAY_LABELS = {"월", "화", "수", "목", "금", "토", "일"};

    private final WorkDayRepository  workDayRepository;
    private final AttendanceService  attendanceService;
    private final ScheduleService    scheduleService;
    private final PointService       pointService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        LocalDate today = LocalDate.now(KST);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        try {
            model.addAttribute("balance", pointService.getBalance(crewId));
        } catch (PointAccountNotFoundException e) {
            model.addAttribute("balance", new PointBalanceResult(0, 0, 0, 0, 0, 0));
        }
        model.addAttribute("pointsPerDay", pointService.getEarnAmount());

        ScheduleResult schedule = scheduleService.getMySchedule(crewId).orElse(null);
        Map<LocalDate, WorkDay> workDaysByDate = workDayRepository
            .findByCrewIdAndWorkDateBetween(crewId, monday, sunday).stream()
            .collect(Collectors.toMap(WorkDay::getWorkDate, Function.identity()));

        List<DayView> weekDays = monday.datesUntil(sunday.plusDays(1))
            .map(date -> toDayView(date, today, schedule, workDaysByDate.get(date)))
            .toList();
        model.addAttribute("weekDays", weekDays);

        model.addAttribute("scheduledDaysLabel", scheduledDaysLabel(schedule));

        return "dashboard";
    }

    @PostMapping("/dashboard/workdays/register")
    public String register(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
                            RedirectAttributes redirectAttributes) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        try {
            attendanceService.registerWorkDay(new RegisterWorkDayCommand(crewId, workDate));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/workdays/cancel")
    public String cancel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        attendanceService.cancelWorkDay(crewId, workDate);
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/workdays/reinstate")
    public String reinstate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        attendanceService.reinstateWorkDay(crewId, workDate);
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/points/use")
    public String usePoint(
            @RequestParam long amount,
            @RequestParam String productName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate usedAt,
            @RequestParam(required = false) String brand,
            RedirectAttributes redirectAttributes) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        try {
            UsePointResult result = pointService.use(
                new UsePointCommand(crewId, amount, productName, usedAt, brand));
            redirectAttributes.addFlashAttribute("usedResult",
                new UsedResultView(result.usedLedgerId(), result.usedAmount(), productName, LocalDate.now(KST)));
        } catch (InsufficientPointException | PointAccountNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/points/preview")
    public String previewUse(
            @RequestParam long amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate usedAt,
            Model model) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        try {
            PointUsePreviewResult preview = pointService.previewUse(crewId, amount, usedAt);
            model.addAttribute("preview", preview);
        } catch (InsufficientPointException | PointAccountNotFoundException e) {
            model.addAttribute("previewError", e.getMessage());
        }
        return "fragments/point-use-preview :: preview";
    }

    @PostMapping("/dashboard/points/cancel")
    public String cancelUse(@RequestParam UUID ledgerId, RedirectAttributes redirectAttributes) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        try {
            pointService.cancelUse(new CancelUseCommand(ledgerId, crewId));
            redirectAttributes.addFlashAttribute("message", "포인트 사용이 취소됐어요.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // ── 요일별 상태 계산 ────────────────────────────
    private static DayView toDayView(LocalDate date, LocalDate today, ScheduleResult schedule, WorkDay workDay) {
        boolean isPast = date.isBefore(today);
        boolean isToday = date.equals(today);
        boolean scheduled = isScheduled(schedule, date);
        boolean registered = workDay != null && !workDay.isSkipped();
        boolean absent = workDay != null && workDay.isSkipped();
        boolean pointGranted = workDay != null && workDay.isPointGranted();

        String label, color, bg;
        if (absent) {
            label = "결근"; color = "#E53935"; bg = "#FFF5F5";
        } else if (isPast && scheduled && registered) {
            label = pointGranted ? "적립 완료" : "적립 예정"; color = "#1B9E5B"; bg = "#F0FFF4";
        } else if (isPast && !scheduled && registered) {
            label = "연장근무"; color = "#1565C0"; bg = "#EFF6FF";
        } else if (isPast && scheduled) {
            label = "미등록"; color = "#E65100"; bg = "#FFFBF0";
        } else if (isToday && scheduled) {
            label = registered ? "오늘 · 등록완료" : "오늘 · 소정근무"; color = "#1B9E5B"; bg = "#F0FFF4";
        } else if (isToday) {
            label = registered ? "오늘 · 연장근무" : "오늘"; color = "#1565C0"; bg = "#EFF6FF";
        } else if (scheduled) {
            label = "소정근무 예정"; color = "#1B9E5B"; bg = "#F8FFFE";
        } else {
            label = registered ? "연장근무 예정" : ""; color = "#1565C0"; bg = "#EFF6FF";
        }

        boolean showRegister = isPast && scheduled && !registered && !absent;
        boolean showReinstate = !isPast && absent;
        boolean showAbsent = !isPast && !absent && scheduled;
        boolean showCancel = !isPast && !absent && !scheduled && registered;

        return new DayView(date, DAY_LABELS[date.getDayOfWeek().getValue() - 1],
            isPast, isToday, scheduled, registered, absent, pointGranted,
            label, color, bg, showRegister, showReinstate, showAbsent, showCancel);
    }

    // ── 소정근무 요일 라벨 (예: "월·화·목") ─────────
    private static final String[] DOW_LABELS_SUN_FIRST = {"일", "월", "화", "수", "목", "금", "토"};

    private static String scheduledDaysLabel(ScheduleResult schedule) {
        if (schedule == null || schedule.daysOfWeek().isEmpty()) return "미설정";
        return schedule.daysOfWeek().stream().sorted()
            .map(d -> DOW_LABELS_SUN_FIRST[d])
            .collect(Collectors.joining("·"));
    }

    private static boolean isScheduled(ScheduleResult schedule, LocalDate date) {
        if (schedule == null) return false;
        if (date.isBefore(schedule.startDate())) return false;
        if (schedule.endDate() != null && date.isAfter(schedule.endDate())) return false;
        int dow = date.getDayOfWeek().getValue() % 7; // 0=일~6=토
        return schedule.daysOfWeek().contains(dow);
    }

    public record DayView(
        LocalDate date, String dayLabel, boolean isPast, boolean isToday,
        boolean scheduled, boolean registered, boolean absent, boolean pointGranted,
        String statusLabel, String statusColor, String statusBg,
        boolean showRegister, boolean showReinstate, boolean showAbsent, boolean showCancel) {}

    public record UsedResultView(UUID ledgerId, long amount, String product, LocalDate usedDate) {}
}
