package com.sample.common.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * === 设计要点 ===
 * Money 值对象：封装金额运算，确保精度安全。
 *
 * 为什么需要 Money 而不是直接用 BigDecimal？
 * 1. 防止 new BigDecimal(0.1) 这种精度丢失的错误
 * 2. 统一精度规则（2位小数，四舍五入）
 * 3. 提供业务语义的加减方法
 * 4. 不可变对象（线程安全，无副作用）
 *
 * 值对象的核心特征：
 * - 通过值相等（equals 比较内容，不是引用）
 * - 不可变（所有字段 final，方法返回新对象）
 * - 自校验（构造时即确保合法性）
 */
public final class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /** 金额值，精确到2位小数 */
    private final BigDecimal amount;

    // ========== 构造方法（自校验） ==========

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 工厂方法：从字符串创建（推荐，避免浮点精度问题）
     * 用法：Money.of("3000.00")
     */
    public static Money of(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("金额不能为空");
        }
        return new Money(new BigDecimal(amount));
    }

    /**
     * 工厂方法：从 BigDecimal 创建
     */
    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        return new Money(amount);
    }

    // ========== 业务运算（返回新对象，不修改自身） ==========

    /** 加法 */
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /** 减法 */
    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    /** 是否大于 */
    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    /** 是否小于零 */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    // ========== 值对象标准方法 ==========

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Money money = (Money) o;
        // 注意：使用 compareTo 而非 equals，因为 BigDecimal 的 equals 会比较精度
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "¥" + amount.toPlainString();
    }
}
