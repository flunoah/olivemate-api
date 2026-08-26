package com.oliveyoung.mate.presentation.notification;

import com.oliveyoung.mate.application.notification.PushSubscriptionService;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.presentation.ApiResponse;
import com.oliveyoung.mate.presentation.SecurityUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/push")
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    public PushSubscriptionController(PushSubscriptionService pushSubscriptionService) {
        this.pushSubscriptionService = pushSubscriptionService;
    }

    @GetMapping("/vapid-public-key")
    public ApiResponse<String> getVapidPublicKey() {
        return ApiResponse.ok(pushSubscriptionService.getPublicKey());
    }

    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(@RequestBody PushSubscribeRequest request) {
        CrewId crewId = CrewId.of(SecurityUtils.authenticatedCrewId()); // ⚠️ CrewId.of(UUID) 존재 확인 필요
        pushSubscriptionService.subscribe(crewId, request.endpoint(), request.p256dh(), request.auth());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/subscribe")
    public ApiResponse<Void> unsubscribe(@RequestParam String endpoint) {
        pushSubscriptionService.unsubscribe(endpoint);
        return ApiResponse.ok(null);
    }
}

record PushSubscribeRequest(String endpoint, String p256dh, String auth) {}