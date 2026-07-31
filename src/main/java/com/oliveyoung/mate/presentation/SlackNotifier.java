package com.oliveyoung.mate.presentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SlackNotifier {

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendError(Exception e, String uri) {
        if (webhookUrl.isEmpty()) return;

        String message = String.format("""
            🚨 *MATE 서버 에러 발생*
            ─────────────────────
            📍 API: %s
            ❌ 에러: %s
            🕐 시간: %s
            ─────────────────────
            즉시 확인 필요!
            """,
            uri,
            e.getMessage(),
            LocalDateTime.now()
        );

        try {
            Map<String, String> body = new HashMap<>();
            body.put("text", message);
            restTemplate.postForObject(webhookUrl, body, String.class);
        } catch (Exception ex) {
            log.error("Slack 알림 발송 실패", ex);
        }
    }

    public void sendSchedulerError(String schedulerName, Exception e) {
        if (webhookUrl.isEmpty()) return;

        String message = String.format("""
            ⏰ *MATE 스케줄러 실패*
            ─────────────────────
            📍 스케줄러: %s
            ❌ 에러: %s
            🕐 시간: %s
            ─────────────────────
            포인트 적립 누락 가능성 있음!
            """,
            schedulerName,
            e.getMessage(),
            LocalDateTime.now()
        );

        try {
            Map<String, String> body = new HashMap<>();
            body.put("text", message);
            restTemplate.postForObject(webhookUrl, body, String.class);
        } catch (Exception ex) {
            log.error("Slack 알림 발송 실패", ex);
        }
    }
}
