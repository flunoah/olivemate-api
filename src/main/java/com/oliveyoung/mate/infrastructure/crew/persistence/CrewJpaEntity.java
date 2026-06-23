package com.oliveyoung.mate.infrastructure.crew.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crew")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewJpaEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID crewId;

    @Column(name = "login_id", length = 50, nullable = false, unique = true)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Role { CREW, STUDENT, TEACHER, ADMIN }

    @Builder
    public CrewJpaEntity(UUID crewId, String loginId, String passwordHash,
                         String name, Role role, boolean isActive) {
        this.crewId       = crewId;
        this.loginId      = loginId;
        this.passwordHash = passwordHash;
        this.name         = name;
        this.role         = role;
        this.isActive     = isActive;
    }
}