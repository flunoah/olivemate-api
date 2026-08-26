package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.point.command.CancelUseCommand;
import com.oliveyoung.mate.application.point.result.LedgerHistoryResult;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import com.oliveyoung.mate.domain.point.model.PointLedger.LedgerType;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HistoryPageController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PointService pointService;

    @GetMapping("/history")
    public String history(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        LocalDate today = LocalDate.now(KST);

        LocalDate base;
        if (year != null && month != null) {
            base = LocalDate.of(year, month, 1);
        } else if (date != null) {
            base = date.withDayOfMonth(1);
        } else {
            base = today.withDayOfMonth(1);
        }

        List<LedgerHistoryResult> ledgers;
        try {
            ledgers = pointService.getLedgerHistory(crewId);
        } catch (PointAccountNotFoundException e) {
            ledgers = List.of();
        }

        Map<LocalDate, List<LedgerHistoryResult>> byGrantedDate = ledgers.stream()
            .filter(l -> l.grantedAt() != null)
            .collect(Collectors.groupingBy(l -> l.grantedAt().toLocalDate()));

        Map<LocalDate, Long> expiringByDate = ledgers.stream()
            .filter(l -> l.remaining() > 0 && l.expiredAt() != null && !l.expiredAt().toLocalDate().isBefore(today))
            .collect(Collectors.groupingBy(l -> l.expiredAt().toLocalDate(), Collectors.summingLong(LedgerHistoryResult::remaining)));

        LocalDate selectedDate = date != null ? date
            : (today.getYear() == base.getYear() && today.getMonth() == base.getMonth() ? today : null);

        LocalDate prev = base.minusMonths(1);
        LocalDate next = base.plusMonths(1);

        model.addAttribute("year", base.getYear());
        model.addAttribute("month", base.getMonthValue());
        model.addAttribute("prevYear", prev.getYear());
        model.addAttribute("prevMonth", prev.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("days", buildDays(base, today, selectedDate, byGrantedDate, expiringByDate));
        model.addAttribute("selectedLedgers", selectedDate != null
            ? buildSelectedLedgers(byGrantedDate.getOrDefault(selectedDate, List.of()), today)
            : List.of());
        model.addAttribute("summary", buildSummary(ledgers, expiringByDate, base));
        model.addAttribute("expiryBanner", nearestExpiry(ledgers, today));
        model.addAttribute("prevMonthLedgers", buildPrevMonthLedgers(ledgers, prev, today));

        return "history";
    }

    @PostMapping("/history/points/cancel")
    public String cancel(
            @RequestParam UUID ledgerId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        try {
            pointService.cancelUse(new CancelUseCommand(ledgerId, crewId));
            redirectAttributes.addFlashAttribute("message", "포인트 사용이 취소됐어요.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        String redirect = "redirect:/history?year=" + year + "&month=" + month;
        return date != null ? redirect + "&date=" + date : redirect;
    }

    // ── 달력 그리드 (일=0~토=6, 앞쪽 빈칸 패딩) ─────
    private static List<DayCell> buildDays(
            LocalDate base, LocalDate today, LocalDate selectedDate,
            Map<LocalDate, List<LedgerHistoryResult>> byGrantedDate,
            Map<LocalDate, Long> expiringByDate) {
        LocalDate first = base;
        LocalDate last = base.withDayOfMonth(base.lengthOfMonth());
        int leadingBlanks = first.getDayOfWeek().getValue() % 7;

        List<DayCell> days = new ArrayList<>();
        for (int i = 0; i < leadingBlanks; i++) days.add(DayCell.empty());

        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            List<LedgerHistoryResult> onDate = byGrantedDate.getOrDefault(d, List.of());
            boolean isToday = d.equals(today);
            boolean isSelected = d.equals(selectedDate);
            String cellClass = isSelected ? "bg-green-600 text-white"
                : isToday ? "bg-green-50 text-green-700"
                : "text-gray-600 hover:bg-gray-50";
            days.add(new DayCell(
                d, d.getDayOfMonth(), false,
                isToday, isSelected, cellClass,
                onDate.stream().anyMatch(l -> l.ledgerType() == LedgerType.INIT),
                onDate.stream().anyMatch(l -> l.ledgerType() == LedgerType.EARN),
                onDate.stream().anyMatch(l -> l.ledgerType() == LedgerType.USE),
                onDate.stream().anyMatch(l -> l.ledgerType() == LedgerType.EXPIRE),
                expiringByDate.containsKey(d)
            ));
        }
        return days;
    }

    // ── 선택한 날짜 상세 (USE는 txId로 합산해 되돌리기 가능 여부까지 계산) ─
    private static List<LedgerRowView> buildSelectedLedgers(List<LedgerHistoryResult> onDate, LocalDate today) {
        List<LedgerRowView> rows = new ArrayList<>();

        onDate.stream()
            .filter(l -> l.ledgerType() != LedgerType.USE)
            .forEach(l -> rows.add(toRow(l, l.amount(), l.createdAt(), null, false)));

        Map<UUID, List<LedgerHistoryResult>> useGroups = onDate.stream()
            .filter(l -> l.ledgerType() == LedgerType.USE)
            .collect(Collectors.groupingBy(LedgerHistoryResult::txId));

        useGroups.values().forEach(group -> {
            LedgerHistoryResult first = group.get(0);
            long totalAmount = group.stream().mapToLong(LedgerHistoryResult::amount).sum();
            boolean cancelable = group.stream().anyMatch(l -> l.createdAt().toLocalDate().equals(today));
            rows.add(toRow(first, totalAmount, first.createdAt(), first.ledgerId(), cancelable));
        });

        rows.sort(Comparator.comparing(LedgerRowView::createdAt).reversed());
        return rows;
    }

    private static LedgerRowView toRow(LedgerHistoryResult l, long amount, LocalDateTime createdAt, UUID ledgerId, boolean cancelable) {
        String icon, label, sign;
        switch (l.ledgerType()) {
            case INIT -> { icon = "🎁"; label = "초기 지급"; sign = "+"; }
            case EARN -> { icon = "✅"; label = "적립"; sign = "+"; }
            case USE -> { icon = "🛍️"; label = "사용"; sign = "-"; }
            default -> { icon = "⏰"; label = "소멸"; sign = "-"; } // EXPIRE
        }
        return new LedgerRowView(icon, label, sign, amount, l.description(), l.brand(), createdAt, ledgerId, cancelable);
    }

    // ── 표시 월 요약 (총 적립/사용/소멸 예정) ───────
    private static SummaryView buildSummary(List<LedgerHistoryResult> ledgers, Map<LocalDate, Long> expiringByDate, LocalDate base) {
        long earned = ledgers.stream()
            .filter(l -> (l.ledgerType() == LedgerType.INIT || l.ledgerType() == LedgerType.EARN) && inMonth(l.grantedAt(), base))
            .mapToLong(LedgerHistoryResult::amount).sum();
        long used = ledgers.stream()
            .filter(l -> l.ledgerType() == LedgerType.USE && inMonth(l.grantedAt(), base))
            .mapToLong(LedgerHistoryResult::amount).sum();
        long expiring = expiringByDate.entrySet().stream()
            .filter(e -> e.getKey().getYear() == base.getYear() && e.getKey().getMonth() == base.getMonth())
            .mapToLong(Map.Entry::getValue).sum();
        return new SummaryView(earned, used, expiring);
    }

    private static boolean inMonth(LocalDateTime dt, LocalDate monthBase) {
        return dt != null && dt.getYear() == monthBase.getYear() && dt.getMonth() == monthBase.getMonth();
    }

    // ── 가장 임박한 만료 배너 (월 무관, 전체 기준) ──
    private static ExpiryBannerView nearestExpiry(List<LedgerHistoryResult> ledgers, LocalDate today) {
        return ledgers.stream()
            .filter(l -> l.remaining() > 0 && l.expiredAt() != null && !l.expiredAt().toLocalDate().isBefore(today))
            .min(Comparator.comparing(LedgerHistoryResult::expiredAt))
            .map(l -> new ExpiryBannerView(l.expiredAt().toLocalDate(), l.remaining(),
                ChronoUnit.DAYS.between(today, l.expiredAt().toLocalDate())))
            .orElse(null);
    }

    // ── 지난달 적립 내역 ─────────────────────────────
    private static List<PrevMonthLedgerView> buildPrevMonthLedgers(List<LedgerHistoryResult> ledgers, LocalDate prevMonth, LocalDate today) {
        return ledgers.stream()
            .filter(l -> (l.ledgerType() == LedgerType.EARN || l.ledgerType() == LedgerType.INIT) && inMonth(l.grantedAt(), prevMonth))
            .sorted(Comparator.comparing(LedgerHistoryResult::grantedAt).reversed())
            .map(l -> new PrevMonthLedgerView(
                l.amount(), l.grantedAt().toLocalDate(), l.createdAt().toLocalDate(),
                l.expiredAt() != null ? dDayLabel(today, l.expiredAt().toLocalDate()) : ""
            ))
            .toList();
    }

    private static String dDayLabel(LocalDate today, LocalDate expiry) {
        long d = ChronoUnit.DAYS.between(today, expiry);
        if (d < 0) return "만료됨";
        if (d == 0) return "D-day";
        return "D-" + d;
    }

    public record DayCell(
        LocalDate date, Integer dayOfMonth, boolean blank, boolean today, boolean selected, String cellClass,
        boolean hasInit, boolean hasEarn, boolean hasUse, boolean hasExpire, boolean hasExpiring) {

        static DayCell empty() {
            return new DayCell(null, null, true, false, false, "", false, false, false, false, false);
        }
    }

    public record LedgerRowView(
        String icon, String label, String sign, long amount, String description, String brand,
        LocalDateTime createdAt, UUID ledgerId, boolean cancelable) {}

    public record SummaryView(long earned, long used, long expiring) {}

    public record ExpiryBannerView(LocalDate expiredAt, long amount, long dDay) {}

    public record PrevMonthLedgerView(long amount, LocalDate grantedDate, LocalDate createdDate, String dDayLabel) {}
}
