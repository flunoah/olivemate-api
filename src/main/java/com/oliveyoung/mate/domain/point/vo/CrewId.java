package com.oliveyoung.mate.domain.point.vo;

import java.util.UUID;

public record CrewId(UUID id) {
    public static CrewId of(UUID id) { return new CrewId(id); }
    public static CrewId newId()     { return new CrewId(UUID.randomUUID()); }
}