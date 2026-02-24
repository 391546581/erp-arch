package com.sample.purchase.domain.vo;

import java.util.Objects;

/**
 * === 设计要点 ===
 * PurchaseOrderId 值对象。
 *
 * 对比 L3 的 SnCode：
 * - SnCode 有格式校验（正则表达式、长度限制）
 * - PurchaseOrderId 只有 null 校验
 *
 * 这就是 L2 和 L3 值对象的典型差异 ——
 * L2 的值对象更简单，因为业务规则本身就更简单。
 */
public final class PurchaseOrderId {

    private final Long value;

    private PurchaseOrderId(Long value) {
        this.value = value;
    }

    public static PurchaseOrderId of(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("采购订单 ID 不能为空");
        }
        return new PurchaseOrderId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PurchaseOrderId that = (PurchaseOrderId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "PO-" + value;
    }
}
