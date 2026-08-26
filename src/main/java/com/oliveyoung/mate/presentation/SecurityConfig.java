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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

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
            );

        return http.build();
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
