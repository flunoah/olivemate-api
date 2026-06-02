package com.oliveyoung.mate.domain.point.vo;

public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다. amount=" + amount);
        }
    }

    public static Money of(long amount) { return new Money(amount); }
    public static Money zero()          { return new Money(0); }

    public Money add(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        if (this.amount < other.amount) {
            throw new IllegalStateException(
                "잔액이 부족합니다. balance=%d, requested=%d".formatted(this.amount, other.amount)
            );
        }
        return new Money(this.amount - other.amount);
    }

    public boolean isGreaterThan(Money other) { return this.amount > other.amount; }
    public boolean isZero()                   { return this.amount == 0; }
}