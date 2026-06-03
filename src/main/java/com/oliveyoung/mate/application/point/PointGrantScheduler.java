package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.point.command.EarnPointCommand;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointGrantScheduler {

    private final WorkDayRepository workDayRepository;
    private final PointService      pointService;

    // workDate < 오늘인 미지급 근무일 처리 — 자연스럽게 1일 지연 지급 정책 적용
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void grantPoints() {
        log.info("[Scheduler] 포인트 지급 시작");

        workDayRepository.findAllNotGranted(LocalDate.now()).forEach(workDay -> {
            try {
                pointService.earn(new EarnPointCommand(
                    workDay.getCrewId(),
                    workDay.getWorkDayId(),
                    workDay.getWorkDate()
                ));
                log.info("[Scheduler] 포인트 지급 완료. crewId={}", workDay.getCrewId());
            } catch (Exception e) {
                log.error("[Scheduler] 포인트 지급 실패. crewId={}", workDay.getCrewId(), e);
            }
        });

        log.info("[Scheduler] 포인트 지급 완료");
    }
}