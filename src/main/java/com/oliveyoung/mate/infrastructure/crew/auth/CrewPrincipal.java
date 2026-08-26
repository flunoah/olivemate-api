package com.oliveyoung.mate.infrastructure.crew.auth;

import com.oliveyoung.mate.domain.crew.model.Crew;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CrewPrincipal implements UserDetails {

    private final UUID crewId;
    private final String loginId;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    private CrewPrincipal(UUID crewId, String loginId, String passwordHash,
                           Collection<? extends GrantedAuthority> authorities, boolean enabled) {
        this.crewId = crewId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public static CrewPrincipal fromCrew(Crew crew) {
        return new CrewPrincipal(
            crew.getCrewId(),
            crew.getLoginId(),
            crew.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_" + crew.getRole().name())),
            crew.isActive()
        );
    }

    public static CrewPrincipal fromJwt(UUID crewId, String role) {
        return new CrewPrincipal(
            crewId, null, null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role)),
            true
        );
    }

    public UUID getCrewId() {
        return crewId;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
