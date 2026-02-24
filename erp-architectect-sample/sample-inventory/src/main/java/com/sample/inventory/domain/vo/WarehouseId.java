package com.sample.inventory.domain.vo;

import java.util.Objects;

/**
 * WarehouseId 值对象
 */
public final class WarehouseId {

    private final Long value;

    private WarehouseId(Long value) {
        this.value = value;
    }

    public static WarehouseId of(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("仓库 ID 不能为空");
        }
        return new WarehouseId(value);
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
        WarehouseId that = (WarehouseId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "WarehouseId[" + value + "]";
    }
}
