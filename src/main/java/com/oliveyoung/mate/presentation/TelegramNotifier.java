package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.application.JobReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Component
public class TelegramNotifier {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    private final RestTemplate restTemplate;

    public TelegramNotifier() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Duration -> milliseconds (int) 로 변환해야 함 (setConnectTimeout/setReadTimeout은 int ms를 받음)
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    // 1라운드: 에러 알림
    // 2라운드: AI 분석 결과 발송
    public void sendError(Exception e, String uri) {
        send(String.format("""
            🚨 <b>MATE 서버 에러 발생</b>
            ─────────────────────
            📍 API: %s
            ❌ 에러: %s
            🕐 시간: %s
            ─────────────────────
            즉시 확인 필요!
            """, escapeHtml(uri), escapeHtml(e.getMessage()), LocalDateTime.now(KST)));

        // 2라운드: AI 분석
        String analysis = analyzeWithAI(e, uri);
        if (!analysis.isEmpty()) send(analysis);
    }

    public void sendSchedulerError(String schedulerName, Exception e) {
        send(String.format("""
            ⏰ <b>MATE 스케줄러 실패</b>
            ─────────────────────
            📍 스케줄러: %s
            ❌ 에러: %s
            🕐 시간: %s
            ─────────────────────
            포인트 적립 누락 가능성 있음!
            """, escapeHtml(schedulerName), escapeHtml(e.getMessage()), LocalDateTime.now(KST)));

        // 2라운드: AI 분석
        String analysis = analyzeWithAI(e, schedulerName);
        if (!analysis.isEmpty()) send(analysis);
    }

    public void sendJobReport(JobReport report) {
        send(String.format("""
            %s <b>MATE %s 완료</b>
            ─────────────────────
            📅 대상일: %s
            ✅ 성공: %d건
            ⏭️ 스킵: %d건
            ⚠️ 실패: %d건
            🕐 완료 시간: %s
            """,
            report.failed() == 0 ? "✅" : "⚠️", escapeHtml(report.jobName()), report.date(),
            report.success(), report.skipped(), report.failed(), LocalDateTime.now(KST)));

        if (report.failed() > 0) {
            send(String.format("""
                ⚠️ <b>MATE %s 일부 실패</b>
                ─────────────────────
                📅 대상일: %s
                ❌ 실패: %d건 (성공 %d건, 스킵 %d건)
                ─────────────────────
                누락 건 확인 필요! (로그: [Admin Cron])
                """, escapeHtml(report.jobName()), report.date(),
                report.failed(), report.success(), report.skipped()));
        }
    }

    // AI 분석 루프
    @SuppressWarnings("unchecked")
    private String analyzeWithAI(Exception e, String context) {
        if (anthropicApiKey == null || anthropicApiKey.isEmpty()) return "";

        try {
            String prompt = String.format("""
                MATE 서비스(Spring Boot + PostgreSQL + Supabase)에서 에러가 발생했어.

                위치: %s
                에러 타입: %s
                에러 메시지: %s

                다음 3가지를 각각 1줄로 답해줘:
                1. 원인
                2. 즉시 해결 방법
                3. 재발 방지 방법
                """,
                context,
                e.getClass().getSimpleName(),
                e.getMessage()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = new HashMap<>();
            body.put("model", "claude-haiku-4-5-20251001");
            body.put("max_tokens", 300);
            body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages",
                request,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.warn("AI 응답 바디가 없습니다.");
                return "";
            }

            // 안전한 파싱: content가 List인지 확인 후 추출
            String aiResult = "";
            Object contentObj = responseBody.get("content");
            if (contentObj instanceof List) {
                List<?> contentList = (List<?>) contentObj;
                if (!contentList.isEmpty() && contentList.get(0) instanceof Map) {
                    Object textObj = ((Map<?,?>) contentList.get(0)).get("text");
                    if (textObj != null) aiResult = textObj.toString();
                }
            } else if (responseBody.get("text") != null) {
                aiResult = responseBody.get("text").toString();
            } else {
                log.debug("AI 응답에서 텍스트를 찾지 못했습니다. 전체 응답: {}", responseBody);
            }

            if (aiResult.isBlank()) return "";

            return String.format("""
                🤖 <b>AI 에러 분석 결과</b>
                ─────────────────────
                %s
                ─────────────────────
                """, escapeHtml(aiResult));

        } catch (Exception ex) {
            log.error("AI 분석 실패", ex);
            return "";
        }
    }

    private void send(String text) {
        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) return;
        try {
            Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML"
            );
            restTemplate.postForObject(
                "https://api.telegram.org/bot" + botToken + "/sendMessage",
                body,
                String.class
            );
        } catch (Exception ex) {
            log.error("Telegram 알림 발송 실패", ex);
        }
    }

    // parse_mode=HTML 전송 시 예외 메시지 등 동적 값에 <, >, & 가 섞이면 파싱 에러가 나므로 이스케이프 필요
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
