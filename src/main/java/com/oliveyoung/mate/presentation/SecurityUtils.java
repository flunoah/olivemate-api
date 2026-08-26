package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.infrastructure.crew.auth.CrewPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID authenticatedCrewId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((CrewPrincipal) auth.getPrincipal()).getCrewId();
    }

    // ADMIN만 허용
    public static void validateAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("관리자 권한이 필요합니다.");
        }
    }

    // 본인 또는 ADMIN만 허용
    public static void validateSelfOrAdmin(UUID requestedCrewId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID authenticatedId = ((CrewPrincipal) auth.getPrincipal()).getCrewId();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !authenticatedId.equals(requestedCrewId)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
    }
}
