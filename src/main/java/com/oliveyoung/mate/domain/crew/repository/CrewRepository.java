package com.oliveyoung.mate.domain.crew.repository;

import com.oliveyoung.mate.domain.crew.model.Crew;
import java.util.Optional;
import java.util.UUID;

public interface CrewRepository {
    Crew save(Crew crew);
    Optional<Crew> findByLoginId(String loginId);
    Optional<Crew> findById(UUID crewId);
    boolean existsByLoginId(String loginId);
}