package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.application.JobReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 봇 토큰/채팅방 ID가 설정된 경우에만 Telegram sendMessage로 발송되는지 검증 */
class TelegramNotifierTest {

    private static final String BOT_TOKEN = "test-token";
    private static final String CHAT_ID   = "-100123456789";
    private static final String SEND_URL  = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
    private static final LocalDate DATE   = LocalDate.of(2026, 8, 1);

    private TelegramNotifier notifier;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        notifier = new TelegramNotifier();
        ReflectionTestUtils.setField(notifier, "botToken", BOT_TOKEN);
        ReflectionTestUtils.setField(notifier, "chatId", CHAT_ID);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(notifier, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("서버 에러는 sendMessage로 발송된다")
    void error_is_sent() {
        server.expect(requestTo(SEND_URL)).andRespond(withSuccess());

        notifier.sendError(new RuntimeException("boom"), "/api/points");

        server.verify();
    }

    @Test
    @DisplayName("스케줄러 실패는 sendMessage로 발송된다")
    void scheduler_error_is_sent() {
        server.expect(requestTo(SEND_URL)).andRespond(withSuccess());

        notifier.sendSchedulerError("포인트 적립", new RuntimeException("boom"));

        server.verify();
    }

    @Test
    @DisplayName("완료 리포트는 sendMessage로 발송된다")
    void report_is_sent() {
        server.expect(requestTo(SEND_URL)).andRespond(withSuccess());

        notifier.sendJobReport(JobReport.of("포인트 적립", DATE, 120, 0));

        server.verify();
    }

    @Test
    @DisplayName("스킵만 있고 실패가 없으면 요청은 1건만 나간다")
    void skipped_only_sends_single_request() {
        server.expect(ExpectedCount.times(1), requestTo(SEND_URL)).andRespond(withSuccess());

        notifier.sendJobReport(new JobReport("주간 근무일 생성", DATE, 30, 12, 0));

        server.verify();
    }

    @Test
    @DisplayName("실패 건이 있으면 리포트+에러 2건이 발송된다")
    void failure_sends_two_requests() {
        server.expect(ExpectedCount.times(2), requestTo(SEND_URL)).andRespond(withSuccess());

        notifier.sendJobReport(new JobReport("포인트 적립", DATE, 118, 0, 2));

        server.verify();
    }

    @Test
    @DisplayName("봇 토큰/채팅방 ID가 없으면 아무 요청도 보내지 않는다")
    void no_credentials_no_request() {
        ReflectionTestUtils.setField(notifier, "botToken", "");
        ReflectionTestUtils.setField(notifier, "chatId", "");

        notifier.sendError(new RuntimeException("boom"), "/api/points");
        notifier.sendJobReport(new JobReport("포인트 적립", DATE, 1, 0, 1));

        server.verify(); // expect 등록이 없으므로 요청이 하나라도 나가면 실패
    }
}
