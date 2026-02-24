package com.sample.inventory.domain.vo;

import java.util.Objects;

/**
 * === 设计要点 ===
 * SnCode 值对象：设备序列号。
 *
 * 这是 L3 模型中值对象的典型示范：
 * 1. 构造时自校验（格式检查）—— 非法的 SN 根本不可能存在于系统中
 * 2. 不可变 —— 一旦创建不可修改
 * 3. 通过值相等 —— 两个 SnCode 内容一样就相等
 *
 * 对比 L2 模型：L2 中的 PurchaseOrderId 只做简单的 null 校验。
 * L3 的值对象包含更丰富的业务规则校验。
 */
public final class SnCode {

    private final String value;

    private SnCode(String value) {
        this.value = value;
    }

    /**
     * 工厂方法：创建 SnCode，包含业务规则校验。
     *
     * 业务规则：
     * - SN 不能为空
     * - SN 长度在 5~50 之间
     * - SN 只能包含字母、数字、横杠
     */
    public static SnCode of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SN 编码不能为空");
        }
        String trimmed = value.trim().toUpperCase();
        if (trimmed.length() < 5 || trimmed.length() > 50) {
            throw new IllegalArgumentException("SN 编码长度必须在 5~50 之间，当前: " + trimmed.length());
        }
        if (!trimmed.matches("^[A-Z0-9\\-]+$")) {
            throw new IllegalArgumentException("SN 编码只能包含大写字母、数字和横杠，当前: " + trimmed);
        }
        return new SnCode(trimmed);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SnCode snCode = (SnCode) o;
        return Objects.equals(value, snCode.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "SN[" + value + "]";
    }
}
