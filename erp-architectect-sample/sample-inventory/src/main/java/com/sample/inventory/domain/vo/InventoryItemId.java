package com.sample.inventory.domain.vo;

import java.util.Objects;

/**
 * === 设计要点 ===
 * InventoryItemId 值对象：库存项的唯一标识。
 * 将 Long 类型的 ID 封装为值对象，避免"基本类型偏执 (Primitive Obsession)"。
 */
public final class InventoryItemId {

    private final Long value;

    private InventoryItemId(Long value) {
        this.value = value;
    }

    public static InventoryItemId of(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("库存项 ID 不能为空");
        }
        return new InventoryItemId(value);
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
        InventoryItemId that = (InventoryItemId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "InventoryItemId[" + value + "]";
    }
}
