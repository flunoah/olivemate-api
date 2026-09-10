package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.infrastructure.crew.auth.CrewUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Thymeleaf 페이지 전용 세션 기반 단일 체인.
     * /api/v1/admin/** 은 세션 없이 X-Admin-Key 헤더만으로 호출되는 운영 도구(수동 배치 트리거)이므로
     * permitAll + CSRF 예외를 유지한다 (AdminController.isUnauthorized()에서 자체 인증).
     */
    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/admin/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/login", "/signup", "/css/**", "/js/**", "/webjars/**", "/api/v1/admin/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("loginId")
                .successHandler(roleBasedSuccessHandler())
                .failureUrl("/login?error")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
            )
            .exceptionHandling(handling -> handling.accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    /**
     * 필터 단(컨트롤러 진입 전) 예외 처리 — GlobalExceptionHandler가 못 보는 영역.
     * 세션 기반 CSRF라 세션 만료 시 토큰도 함께 무효화되므로 CSRF 실패는 "세션 만료"로 안내한다.
     * 그 외(순수 권한 부족, 예: 크루가 /admin 진입)는 전용 안내 없이 역할 기반 홈으로 되돌린다.
     * ponytail: 후자는 조용히 리다이렉트만 함, 안내 문구가 필요해지면 쿼리 파라미터 추가
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            String location = (ex instanceof CsrfException) ? "/login?error=expired" : "/";
            if ("true".equals(request.getHeader("HX-Request"))) {
                response.setHeader("HX-Redirect", location);
            } else {
                response.sendRedirect(location);
            }
        };
    }

    /**
     * 로그인 성공 시 계정 역할에 따라 목적지를 분기한다.
     * ADMIN은 /admin, 그 외 크루는 홈 역할의 /dashboard로 이동.
     */
    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            response.sendRedirect(isAdmin ? "/admin" : "/dashboard");
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            CrewUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
