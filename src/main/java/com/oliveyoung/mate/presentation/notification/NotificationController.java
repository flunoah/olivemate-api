package com.oliveyoung.mate.presentation.notification;

import com.oliveyoung.mate.application.notification.NotificationQueryService;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.presentation.ApiResponse;
import com.oliveyoung.mate.presentation.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
        @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        CrewId crewId = CrewId.of(SecurityUtils.authenticatedCrewId());
        var results = notificationQueryService.getNotifications(crewId, unreadOnly);
        return ApiResponse.ok(results.stream().map(NotificationResponse::from).toList());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        CrewId crewId = CrewId.of(SecurityUtils.authenticatedCrewId());
        notificationQueryService.markAsRead(id, crewId);
        return ApiResponse.ok(null);
    }
}