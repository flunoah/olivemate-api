package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.notification.NotificationQueryService;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.infrastructure.crew.auth.CrewPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 하단 네비 알림 탭 배지용 미확인 알림 여부를 모든 web 페이지 컨트롤러에 주입한다.
 * 로그인 전(익명 사용자)에는 CrewPrincipal이 없으므로 false로 둔다.
 */
@ControllerAdvice(basePackages = "com.oliveyoung.mate.presentation.web")
@RequiredArgsConstructor
public class NavBadgeModelAdvice {

    private final NotificationQueryService notificationQueryService;

    @ModelAttribute("hasUnreadNotifications")
    public boolean hasUnreadNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CrewPrincipal principal)) {
            return false;
        }
        return !notificationQueryService.getNotifications(CrewId.of(principal.getCrewId()), true).isEmpty();
    }
}
