package com.oliveyoung.mate.infrastructure.notification.persistence;

import com.oliveyoung.mate.domain.notification.model.Notification;
import com.oliveyoung.mate.domain.notification.repository.NotificationRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationMapper mapper;

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity saved = jpaRepository.save(mapper.toJpa(notification));
        return mapper.toDomain(saved);
    }

    @Override
    public List<Notification> findByCrewId(CrewId crewId, boolean unreadOnly) {
        List<NotificationJpaEntity> entities = unreadOnly
            ? jpaRepository.findByCrewIdAndReadFalseOrderBySentAtDesc(crewId.id())
            : jpaRepository.findByCrewIdOrderBySentAtDesc(crewId.id());
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void markAsRead(UUID id, CrewId crewId) {
        jpaRepository.findByIdAndCrewId(id, crewId.id())
            .ifPresent(entity -> entity.markAsRead()); // 영속 상태이므로 dirty checking으로 flush
    }
}