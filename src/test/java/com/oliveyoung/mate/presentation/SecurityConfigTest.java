package com.oliveyoung.mate.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final AccessDeniedHandler handler = new SecurityConfig().accessDeniedHandler();

    @Test
    void csrf실패는_세션만료로_로그인페이지로_리다이렉트() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        handler.handle(request, response, new CsrfException("token missing"));

        verify(response).sendRedirect("/login?error=expired");
    }

    @Test
    void csrf실패_htmx요청은_HX_Redirect_헤더로_응답() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("HX-Request")).thenReturn("true");

        handler.handle(request, response, new CsrfException("token missing"));

        verify(response).setHeader("HX-Redirect", "/login?error=expired");
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void 순수_권한부족은_홈으로_리다이렉트() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        handler.handle(request, response, new AccessDeniedException("접근 거부"));

        verify(response).sendRedirect("/");
    }
}
