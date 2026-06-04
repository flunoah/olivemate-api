package com.oliveyoung.mate.infrastructure.crew.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrewJpaRepository
        extends JpaRepository<CrewJpaEntity, UUID> {

    Optional<CrewJpaEntity> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    List<CrewJpaEntity> findAllByIsActiveTrue();
}