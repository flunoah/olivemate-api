package com.oliveyoung.mate.infrastructure.crew.persistence;

import com.oliveyoung.mate.domain.crew.model.Crew;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CrewRepositoryImpl implements CrewRepository {

    private final CrewJpaRepository crewJpaRepository;

    @Override
    public Crew save(Crew crew) {
        CrewJpaEntity entity = CrewJpaEntity.builder()
            .crewId(crew.getCrewId())
            .loginId(crew.getLoginId())
            .passwordHash(crew.getPasswordHash())
            .name(crew.getName())
            .role(CrewJpaEntity.Role.valueOf(crew.getRole().name()))
            .isActive(crew.isActive())
            .build();
        crewJpaRepository.save(entity);
        return crew;
    }

    @Override
    public Optional<Crew> findByLoginId(String loginId) {
        return crewJpaRepository.findByLoginId(loginId).map(this::toDomain);
    }

    @Override
    public Optional<Crew> findById(UUID crewId) {
        return crewJpaRepository.findById(crewId).map(this::toDomain);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return crewJpaRepository.existsByLoginId(loginId);
    }

    @Override
    public List<Crew> findAllActive() {
        return crewJpaRepository.findAllByIsActiveTrue().stream()
            .map(this::toDomain)
            .toList();
    }

    private Crew toDomain(CrewJpaEntity e) {
        return Crew.of(
            e.getCrewId(), e.getLoginId(), e.getPasswordHash(),
            e.getName(), Crew.Role.valueOf(e.getRole().name()),
            e.getCreatedAt(), e.isActive()
        );
    }
}