package com.oliveyoung.mate.application.notification;

import com.oliveyoung.mate.application.notification.result.NotificationResult;
import com.oliveyoung.mate.domain.notification.repository.NotificationRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResult> getNotifications(CrewId crewId, boolean unreadOnly) {
        return notificationRepository.findByCrewId(crewId, unreadOnly).stream()
            .map(NotificationResult::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(UUID id, CrewId crewId) {
        notificationRepository.markAsRead(id, crewId);
    }
}