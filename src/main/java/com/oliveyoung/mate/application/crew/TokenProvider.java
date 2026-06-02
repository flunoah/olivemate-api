package com.oliveyoung.mate.application.crew;

import java.util.UUID;

public interface TokenProvider {
    String generate(UUID crewId, String role);
    long expireSeconds();
}
