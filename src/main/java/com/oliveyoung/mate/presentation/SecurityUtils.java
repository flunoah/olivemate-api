package com.oliveyoung.mate.presentation;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID authenticatedCrewId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 본인 또는 ADMIN만 허용
    public static void validateSelfOrAdmin(UUID requestedCrewId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID authenticatedId = (UUID) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !authenticatedId.equals(requestedCrewId)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
    }
}
