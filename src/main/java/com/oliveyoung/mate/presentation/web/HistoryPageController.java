package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.point.command.CancelUseCommand;
import com.oliveyoung.mate.application.point.result.LedgerHistoryResult;
import com.oliveyoung.mate.application.point.result.PointCancelPreviewResult;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import com.oliveyoung.mate.domain.point.model.PointLedger.LedgerType;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
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
    public String history(@RequestParam(required = false, defaultValue = "ALL") String type, Model model) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        LocalDate today = LocalDate.now(KST);

        List<LedgerHistoryResult> ledgers;
        try {
            ledgers = pointService.getLedgerHistory(crewId);
        } catch (PointAccountNotFoundException e) {
            ledgers = List.of();
        }

        Map<LocalDate, Long> expiringByDate = ledgers.stream()
            .filter(l -> l.remaining() > 0 && l.expiredAt() != null && !l.expiredAt().toLocalDate().isBefore(today))
            .collect(Collectors.groupingBy(l -> l.expiredAt().toLocalDate(), Collectors.summingLong(LedgerHistoryResult::remaining)));

        model.addAttribute("type", type);
        model.addAttribute("groups", buildDateGroups(filterByType(ledgers, type), today));
        model.addAttribute("summary", buildSummary(ledgers, expiringByDate, today.withDayOfMonth(1)));
        model.addAttribute("expiryBanner", nearestExpiry(ledgers, today));

        return "history";
    }

    @PostMapping("/history/points/cancel/preview")
    public String previewCancel(@RequestParam UUID ledgerId, Model model) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        try {
            PointCancelPreviewResult preview = pointService.previewCancel(crewId, ledgerId);
            model.addAttribute("preview", preview);
        } catch (IllegalArgumentException e) {
            model.addAttribute("previewError", e.getMessage());
        }
        model.addAttribute("ledgerId", ledgerId);
        return "fragments/point-cancel-preview :: preview";
    }

    @PostMapping("/history/points/cancel")
    public String cancel(
            @RequestParam UUID ledgerId,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            RedirectAttributes redirectAttributes) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        try {
            pointService.cancelUse(new CancelUseCommand(ledgerId, crewId));
            redirectAttributes.addFlashAttribute("message", "포인트 사용이 취소됐어요.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/history?type=" + type;
    }

    // ── 필터 탭(전체/적립/사용/소멸) ─────────────────
    private static List<LedgerHistoryResult> filterByType(List<LedgerHistoryResult> ledgers, String type) {
        return switch (type) {
            case "EARN" -> ledgers.stream().filter(l -> l.ledgerType() == LedgerType.EARN || l.ledgerType() == LedgerType.INIT).toList();
            case "USE" -> ledgers.stream().filter(l -> l.ledgerType() == LedgerType.USE).toList();
            case "EXPIRE" -> ledgers.stream().filter(l -> l.ledgerType() == LedgerType.EXPIRE).toList();
            default -> ledgers;
        };
    }

    // ── 날짜별 그룹(최신 날짜 순) — grantedAt이 실제 표시 기준일 ─
    // (USE는 grantedAt에 usedAt이, EXPIRE는 expiredAt이 그대로 들어있다 — PointLedger 참고)
    private static List<DateGroupView> buildDateGroups(List<LedgerHistoryResult> ledgers, LocalDate today) {
        Map<LocalDate, List<LedgerHistoryResult>> byDate = ledgers.stream()
            .collect(Collectors.groupingBy(l -> l.grantedAt().toLocalDate()));

        return byDate.keySet().stream()
            .sorted(Comparator.reverseOrder())
            .map(d -> new DateGroupView(dateHeader(d, today), buildSelectedLedgers(byDate.get(d), today)))
            .toList();
    }

    private static String dateHeader(LocalDate date, LocalDate today) {
        String label = date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
        return date.equals(today) ? "오늘 · " + label : label;
    }

    // ── 같은 날짜의 원장 목록 (USE는 txId로 합산해 되돌리기 가능 여부까지 계산) ─
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

    public record DateGroupView(String dateLabel, List<LedgerRowView> rows) {}

    public record LedgerRowView(
        String icon, String label, String sign, long amount, String description, String brand,
        LocalDateTime createdAt, UUID ledgerId, boolean cancelable) {}

    public record SummaryView(long earned, long used, long expiring) {}

    public record ExpiryBannerView(LocalDate expiredAt, long amount, long dDay) {}
}
