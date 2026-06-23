package com.oliveyoung.mate.domain.crew.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Crew {

    public enum Role { CREW, STUDENT, TEACHER, ADMIN }

    private final UUID          crewId;
    private final String        loginId;
    private final String        passwordHash;
    private final String        name;
    private final Role          role;
    private final LocalDateTime createdAt;
    private final boolean       isActive;

    private Crew(UUID crewId, String loginId, String passwordHash,
                 String name, Role role, LocalDateTime createdAt,
                 boolean isActive) {
        this.crewId       = crewId;
        this.loginId      = loginId;
        this.passwordHash = passwordHash;
        this.name         = name;
        this.role         = role;
        this.createdAt    = createdAt;
        this.isActive     = isActive;
    }

    public static Crew create(String loginId, String passwordHash, String name, Role role) {
        return new Crew(
            UUID.randomUUID(), loginId, passwordHash, name,
            role, LocalDateTime.now(), true
        );
    }

    public static Crew of(UUID crewId, String loginId, String passwordHash,
                          String name, Role role, LocalDateTime createdAt,
                          boolean isActive) {
        return new Crew(crewId, loginId, passwordHash, name,
                        role, createdAt, isActive);
    }

    public UUID          getCrewId()       { return crewId; }
    public String        getLoginId()      { return loginId; }
    public String        getPasswordHash() { return passwordHash; }
    public String        getName()         { return name; }
    public Role          getRole()         { return role; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public boolean       isActive()        { return isActive; }
}