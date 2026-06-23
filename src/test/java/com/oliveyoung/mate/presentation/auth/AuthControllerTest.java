package com.oliveyoung.mate.presentation.auth;

import com.oliveyoung.mate.application.crew.CrewService;
import com.oliveyoung.mate.application.crew.result.TokenResult;
import com.oliveyoung.mate.presentation.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    CrewService crewService;

    MockMvc mockMvc;

    private static final TokenResult FAKE_TOKEN =
        TokenResult.of("access.token.value", "refresh.token.value", 86400L);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(crewService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // ── 로그인 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/login — 정상 로그인 → 200 + 토큰")
    void login_success() throws Exception {
        given(crewService.login(any())).willReturn(FAKE_TOKEN);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "2920001533209", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("access.token.value"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — 비밀번호 틀림 → 400")
    void login_wrongPassword() throws Exception {
        given(crewService.login(any()))
            .willThrow(new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다."));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "2920001533209", "password": "wrongpassword"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — email 빈값 → 400 (Validation)")
    void login_emptyEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "", "password": "password123"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — password 빈값 → 400 (Validation)")
    void login_emptyPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "2920001533209", "password": ""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // ── 회원가입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — 정상 가입 → 200 + 토큰")
    void register_success() throws Exception {
        given(crewService.signUp(any())).willReturn(FAKE_TOKEN);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "9990001111111", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — 중복 이메일 → 409")
    void register_duplicateEmail() throws Exception {
        given(crewService.signUp(any()))
            .willThrow(new IllegalStateException("이미 사용 중인 이메일입니다."));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "2920001533209", "password": "password123"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — 비밀번호 8자 미만 → 400 (Validation)")
    void register_shortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "2920001533209", "password": "short"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // ── 토큰 갱신 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/refresh — 정상 갱신 → 200 + 새 토큰")
    void refresh_success() throws Exception {
        given(crewService.refresh(any())).willReturn(FAKE_TOKEN);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("X-Refresh-Token", "valid.refresh.token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("access.token.value"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh — 유효하지 않은 토큰 → 400")
    void refresh_invalidToken() throws Exception {
        given(crewService.refresh(any()))
            .willThrow(new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("X-Refresh-Token", "bad.token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
