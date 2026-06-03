package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpiryScheduler {

    private final PointRepository pointRepository;
    private final PointService    pointService;

    // 포인트 지급(00:00)이 완전히 끝난 뒤 실행되도록 1분 뒤로 분리
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void expirePoints() {
        log.info("[Scheduler] 포인트 만료 처리 시작");

        pointRepository.findAllCrewIdsWithExpiringPoints()
            .forEach(crewId -> {
                try {
                    pointService.expirePoints(crewId.id());
                } catch (Exception e) {
                    log.error("[Scheduler] 만료 처리 실패. crewId={}", crewId.id(), e);
                }
            });

        log.info("[Scheduler] 포인트 만료 처리 완료");
    }
}