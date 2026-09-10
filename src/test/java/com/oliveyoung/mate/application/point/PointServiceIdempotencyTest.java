package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.point.command.UsePointCommand;
import com.oliveyoung.mate.application.point.result.UsePointResult;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.point.model.Point;
import com.oliveyoung.mate.domain.point.repository.PointPolicyRepository;
import com.oliveyoung.mate.domain.point.repository.PointRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.point.vo.PointPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 사용(use) 요청 멱등성 검증 (PointService.use(), PointService.java:84).
 * 더블클릭·네트워크 재시도로 같은 idempotencyKey가 두 번 오면, 두 번째 요청은
 * FIFO 재차감 없이 첫 번째 결과를 그대로 반환해야 한다.
 */
class PointServiceIdempotencyTest {

    private PointRepository pointRepository;
    private PointService    pointService;

    private final CrewId crewId         = CrewId.newId();
    private final UUID   idempotencyKey = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pointRepository = mock(PointRepository.class);

        pointService = new PointService(
            pointRepository,
            mock(PointPolicyRepository.class),
            mock(WorkDayRepository.class),
            mock(ApplicationEventPublisher.class),
            mock(TransactionTemplate.class)
        );
    }

    private Point pointWithBalance(long amount) {
        Point point = Point.create(crewId);
        point.earn(new PointPolicy(amount, 1, 31), UUID.randomUUID(),
            LocalDateTime.now(), LocalDateTime.now().plusDays(5));
        return point;
    }

    @Test
    @DisplayName("동일 idempotencyKey로 두 번 요청하면 두 번째는 재차감 없이 첫 결과를 그대로 반환한다")
    void duplicate_request_with_same_idempotency_key_does_not_deduct_twice() {
        Point point = pointWithBalance(1000);
        when(pointRepository.findByCrewId(crewId)).thenReturn(Optional.of(point));

        ArgumentCaptor<UUID> txIdCaptor = ArgumentCaptor.forClass(UUID.class);
        when(pointRepository.registerUseRequestIfAbsent(eq(crewId), eq(idempotencyKey), txIdCaptor.capture()))
            .thenReturn(true, false);

        UsePointCommand cmd = new UsePointCommand(crewId.id(), 300, "desc", null, null, idempotencyKey);

        UsePointResult first = pointService.use(cmd);

        UUID firstTxId = txIdCaptor.getValue();
        when(pointRepository.findTxIdByIdempotencyKey(crewId, idempotencyKey))
            .thenReturn(Optional.of(firstTxId));

        UsePointResult second = pointService.use(cmd);

        assertThat(second.usedAmount()).isEqualTo(first.usedAmount());
        assertThat(second.usedLedgerId()).isEqualTo(first.usedLedgerId());
        assertThat(point.getBalance()).isEqualTo(Money.of(700)); // 300만 차감 — 두 번째 요청은 재차감 안 됨
        verify(pointRepository, times(1)).save(any());
    }
}
