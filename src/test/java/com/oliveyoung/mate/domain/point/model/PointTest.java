package com.oliveyoung.mate.domain.point.model;

import com.oliveyoung.mate.domain.point.InsufficientPointException;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.point.vo.PointPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FIFO 차감/만료/취소 로직 단위 테스트 (Point.java:72,103,124).
 * DB 의존이 없는 순수 도메인 객체라 Mockito 없이 직접 검증한다.
 */
class PointTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);

    private CrewId crewId;

    @BeforeEach
    void setUp() {
        crewId = CrewId.newId();
    }

    private void earn(Point point, long amount, LocalDateTime grantedAt, LocalDateTime expiredAt) {
        point.earn(new PointPolicy(amount, 1, 31), UUID.randomUUID(), grantedAt, expiredAt);
    }

    private List<PointLedger> earnLedgersOf(Point point) {
        return point.getLedgers().stream()
            .filter(l -> l.getType() == PointLedger.LedgerType.EARN)
            .toList();
    }

    @Test
    @DisplayName("여러 원장에 걸쳐 만료일이 빠른 순서대로 FIFO 차감된다")
    void use_deducts_fifo_across_multiple_ledgers() {
        Point point = Point.create(crewId);
        earn(point, 1000, NOW, NOW.plusDays(5));
        earn(point, 1000, NOW, NOW.plusDays(10));

        point.use(Money.of(1500), UUID.randomUUID(), NOW, "desc", "brand");

        List<PointLedger> earnLedgers = earnLedgersOf(point);
        assertThat(earnLedgers.get(0).getRemaining()).isEqualTo(Money.zero());
        assertThat(earnLedgers.get(1).getRemaining()).isEqualTo(Money.of(500));
        assertThat(point.getBalance()).isEqualTo(Money.of(500));

        List<PointLedger> useLedgers = point.getLedgers().stream()
            .filter(l -> l.getType() == PointLedger.LedgerType.USE)
            .toList();
        assertThat(useLedgers).hasSize(2);
        assertThat(useLedgers.stream().mapToLong(l -> l.getAmount().amount()).sum()).isEqualTo(1500);
    }

    @Test
    @DisplayName("만료일이 같은 원장들은 먼저 적립된 순서(리스트 앞쪽)대로 차감된다")
    void use_breaks_tie_by_insertion_order_when_expiry_dates_are_equal() {
        Point point = Point.create(crewId);
        LocalDateTime sameExpiry = NOW.plusDays(5);
        earn(point, 500, NOW, sameExpiry);
        earn(point, 500, NOW, sameExpiry);

        point.use(Money.of(700), UUID.randomUUID(), NOW, "desc", "brand");

        List<PointLedger> earnLedgers = earnLedgersOf(point);
        assertThat(earnLedgers.get(0).getRemaining()).isEqualTo(Money.zero());
        assertThat(earnLedgers.get(1).getRemaining()).isEqualTo(Money.of(300));
    }

    @Test
    @DisplayName("만료일이 없는(null) 원장은 만료일이 있는 원장보다 뒤에 차감된다")
    void use_treats_null_expiry_as_last_in_line() {
        Point point = Point.create(crewId);
        earn(point, 500, NOW, null);
        earn(point, 500, NOW, NOW.plusDays(5));

        point.use(Money.of(700), UUID.randomUUID(), NOW, "desc", "brand");

        List<PointLedger> earnLedgers = earnLedgersOf(point);
        assertThat(earnLedgers.get(0).getRemaining()).isEqualTo(Money.of(300)); // null 만료 → 뒤로 밀림
        assertThat(earnLedgers.get(1).getRemaining()).isEqualTo(Money.zero());  // 만료일 있는 쪽 먼저 소진
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 요청하면 InsufficientPointException이 발생한다")
    void use_throws_when_balance_is_insufficient() {
        Point point = Point.create(crewId);
        earn(point, 500, NOW, NOW.plusDays(5));

        assertThatThrownBy(() -> point.use(Money.of(600), UUID.randomUUID(), NOW, "desc", "brand"))
            .isInstanceOf(InsufficientPointException.class);
    }

    @Test
    @DisplayName("[알려진 버그] 만료 배치가 아직 안 돈 원장을 사용하면 실제 차감 없이 balance만 줄어든다")
    void use_against_already_expired_ledger_does_not_deduct_but_still_reduces_balance() {
        // ponytail: expireOld()가 아직 안 돈 상태에서 이미 만료 시각이 지난 원장만 있으면
        // FIFO 대상 필터(!isExpired)에서 전부 제외돼 실제로는 아무 원장도 차감되지 않는데,
        // balance는 무조건 차감되고 USE 원장도 생기지 않아 거래 내역과 잔액이 어긋난다.
        // 수정은 이번 플랜 범위 밖이라 TODO.md에 별도 항목으로 기록 예정.
        Point point = Point.create(crewId);
        earn(point, 500, NOW.minusDays(10), NOW.minusDays(1));

        point.use(Money.of(500), UUID.randomUUID(), NOW, "desc", "brand");

        boolean anyUseLedgerCreated = point.getLedgers().stream()
            .anyMatch(l -> l.getType() == PointLedger.LedgerType.USE);
        assertThat(anyUseLedgerCreated).isFalse();
        assertThat(point.getBalance()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("cancelUse는 차감했던 원장들에 FIFO 순서로 분산 복원한다")
    void cancel_use_restores_across_multiple_ledgers_in_fifo_order() {
        Point point = Point.create(crewId);
        earn(point, 500, NOW, NOW.plusDays(5));
        earn(point, 500, NOW, NOW.plusDays(10));
        UUID txId = UUID.randomUUID();
        point.use(Money.of(700), txId, NOW, "desc", "brand");

        point.cancelUse(txId);

        List<PointLedger> earnLedgers = earnLedgersOf(point);
        assertThat(earnLedgers.get(0).getRemaining()).isEqualTo(Money.of(500));
        assertThat(earnLedgers.get(1).getRemaining()).isEqualTo(Money.of(500));
        assertThat(point.getBalance()).isEqualTo(Money.of(1000));
    }

    @Test
    @DisplayName("취소할 사용 내역이 없으면 IllegalArgumentException이 발생한다")
    void cancel_use_throws_when_no_matching_use_ledger_exists() {
        Point point = Point.create(crewId);

        assertThatThrownBy(() -> point.cancelUse(UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[알려진 버그] 동일 txId를 두 번 취소하면 원장 복원 없이 balance만 중복으로 늘어난다")
    void cancel_use_twice_with_same_tx_id_inflates_balance_without_restoring_ledgers() {
        // ponytail: cancelUse는 USE 원장의 존재 여부만 확인할 뿐 "이미 취소됐는지"는 확인하지 않는다.
        // 두 번째 호출 시 복원 대상(amount>remaining)이 이미 없어 실제 원장 복원은 0건이지만
        // balance는 totalToRestore만큼 또 증가해 원장 합계와 balance가 어긋난다.
        // 수정은 이번 플랜 범위 밖이라 TODO.md에 별도 항목으로 기록 예정.
        Point point = Point.create(crewId);
        earn(point, 500, NOW, NOW.plusDays(5));
        UUID txId = UUID.randomUUID();
        point.use(Money.of(500), txId, NOW, "desc", "brand");

        point.cancelUse(txId);
        Money balanceAfterFirstCancel = point.getBalance();
        point.cancelUse(txId);

        assertThat(balanceAfterFirstCancel).isEqualTo(Money.of(500));
        assertThat(point.getBalance()).isEqualTo(Money.of(1000));
    }
}
