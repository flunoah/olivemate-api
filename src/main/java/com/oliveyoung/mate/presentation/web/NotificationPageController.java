package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.notification.NotificationQueryService;
import com.oliveyoung.mate.application.notification.result.NotificationResult;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationPageController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    public String notificationsPage(Model model) {
        CrewId crewId = CrewId.of(SecurityUtils.authenticatedCrewId());
        model.addAttribute("notifications",
            notificationQueryService.getNotifications(crewId, false).stream()
                .map(NotificationPageController::toViewItem)
                .toList());
        return "notifications";
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @RequestParam(required = false) String deepLink) {
        CrewId crewId = CrewId.of(SecurityUtils.authenticatedCrewId());
        notificationQueryService.markAsRead(id, crewId);
        String redirectTo = (deepLink == null || deepLink.isBlank()) ? "/dashboard" : deepLink;
        return ResponseEntity.noContent().header("HX-Redirect", redirectTo).build();
    }

    private record NotificationViewItem(
        UUID id, String emoji, String title, String body,
        String deepLink, boolean read, String timeAgo) {}

    private static NotificationViewItem toViewItem(NotificationResult r) {
        String emoji = switch (r.type()) {
            case "POINT_EXPIRING" -> "⏰";
            case "ADMIN_ADJUSTED" -> "🛠️";
            default -> "🎉";
        };
        return new NotificationViewItem(r.id(), emoji, r.title(), r.body(),
            r.deepLink(), r.read(), timeAgo(r.sentAt()));
    }

    private static String timeAgo(LocalDateTime sentAt) {
        Duration d = Duration.between(sentAt, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        long minutes = d.toMinutes();
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";
        long hours = d.toHours();
        if (hours < 24) return hours + "시간 전";
        return d.toDays() + "일 전";
    }
}
