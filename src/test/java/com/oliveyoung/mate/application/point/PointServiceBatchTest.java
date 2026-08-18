package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.JobReport;
import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.point.repository.PointPolicyRepository;
import com.oliveyoung.mate.domain.point.repository.PointRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 일괄 적립의 트랜잭션 경계 검증.
 *
 * <p>earn()의 @Transactional은 grantPointsForAll()에서 자기 호출되므로 프록시를 타지 않는다.
 * TransactionTemplate으로 항목별 트랜잭션을 직접 열지 않으면 save()와 markPointGranted()가
 * 따로 커밋돼, 후자가 실패했을 때 포인트만 적립되고 pointGranted=false로 남는다.
 * 그러면 다음날 크론이 같은 근무일을 다시 집어 이중 적립된다.
 */
class PointServiceBatchTest {

    private PointRepository       pointRepository;
    private PointPolicyRepository policyRepository;
    private WorkDayRepository     workDayRepository;
    private PlatformTransactionManager txManager;
    private PointService          pointService;

    private static final LocalDate WORK_DATE = LocalDate.of(2026, 7, 31);

    @BeforeEach
    void setUp() {
        pointRepository   = mock(PointRepository.class);
        policyRepository  = mock(PointPolicyRepository.class);
        workDayRepository = mock(WorkDayRepository.class);
        txManager         = mock(PlatformTransactionManager.class);

        when(policyRepository.findActivePolicy()).thenReturn(Optional.empty()); // → defaultPolicy
        when(pointRepository.findByCrewId(any(CrewId.class))).thenReturn(Optional.empty());
        when(txManager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());

        pointService = new PointService(
            pointRepository,
            policyRepository,
            workDayRepository,
            mock(ApplicationEventPublisher.class),
            new TransactionTemplate(txManager)
        );
    }

    private WorkDay workDay() {
        return WorkDay.reconstitute(
            UUID.randomUUID(), UUID.randomUUID(), WORK_DATE, false, false, LocalDateTime.now());
    }

    @Test
    @DisplayName("적립 건마다 트랜잭션이 하나씩 열리고 커밋된다")
    void each_item_runs_in_its_own_transaction() {
        when(workDayRepository.findAllNotGranted(any()))
            .thenReturn(List.of(workDay(), workDay(), workDay()));

        JobReport report = pointService.grantPointsForAll();

        assertThat(report.success()).isEqualTo(3);
        assertThat(report.failed()).isZero();
        verify(txManager, times(3)).getTransaction(any());
        verify(txManager, times(3)).commit(any(TransactionStatus.class));
        verify(txManager, never()).rollback(any(TransactionStatus.class));
    }

    @Test
    @DisplayName("지급 표시(markPointGranted)가 실패하면 해당 건은 롤백된다 — 이중 적립 방지")
    void failed_mark_rolls_back_the_whole_item() {
        WorkDay failing = workDay();
        when(workDayRepository.findAllNotGranted(any()))
            .thenReturn(List.of(workDay(), failing, workDay()));
        doThrow(new RuntimeException("DB down"))
            .when(workDayRepository).markPointGranted(failing.getWorkDayId());

        JobReport report = pointService.grantPointsForAll();

        // 실패 건은 롤백 1회, 나머지 2건은 정상 커밋 — 부분 커밋이 남지 않는다
        verify(txManager, times(1)).rollback(any(TransactionStatus.class));
        verify(txManager, times(2)).commit(any(TransactionStatus.class));
        assertThat(report.success()).isEqualTo(2);
        assertThat(report.failed()).isEqualTo(1);
    }

    @Test
    @DisplayName("한 건이 실패해도 나머지 건은 계속 처리된다")
    void one_failure_does_not_abort_the_batch() {
        WorkDay failing = workDay();
        WorkDay after   = workDay();
        when(workDayRepository.findAllNotGranted(any()))
            .thenReturn(List.of(failing, after));
        doThrow(new RuntimeException("DB down"))
            .when(workDayRepository).markPointGranted(failing.getWorkDayId());

        pointService.grantPointsForAll();

        verify(workDayRepository).markPointGranted(after.getWorkDayId());
    }

    @Test
    @DisplayName("만료 대상이 없으면 트랜잭션을 열지 않고 0건 리포트를 낸다")
    void empty_batch_opens_no_transaction() {
        when(pointRepository.findAllCrewIdsWithExpiringPoints()).thenReturn(List.of());

        JobReport report = pointService.expireAllPoints();

        assertThat(report.success()).isZero();
        assertThat(report.failed()).isZero();
        verify(txManager, never()).getTransaction(any());
    }
}
