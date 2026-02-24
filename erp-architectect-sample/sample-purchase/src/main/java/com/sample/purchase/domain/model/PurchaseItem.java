package com.sample.purchase.domain.model;

import com.sample.common.vo.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * === 设计要点 ===
 * PurchaseItem 实体 — 采购明细行。
 *
 * 这是"受聚合根保护的实体"的示范：
 * - PurchaseItem 不能被外部直接创建或修改
 * - 只能通过 PurchaseOrder 的方法来操作
 * - 这保证了聚合根对内部一致性的控制
 *
 * 对比 L3 的 InventoryItem：
 * - InventoryItem 本身就是聚合根
 * - PurchaseItem 是聚合根(PurchaseOrder)的内部实体
 * - 内部实体不能独立存在，必须依附于聚合根
 */
public class PurchaseItem {

    private Long skuId;
    private Integer quantity;
    private Money unitPrice;
    private Integer receivedQty;
    private final List<String> receivedSnCodes;

    /**
     * 包级私有构造 — 只允许 PurchaseOrder 创建。
     */
    PurchaseItem(Long skuId, Integer quantity, Money unitPrice) {
        if (skuId == null)
            throw new IllegalArgumentException("SKU ID 不能为空");
        if (quantity == null || quantity <= 0)
            throw new IllegalArgumentException("采购数量必须大于零");
        if (unitPrice == null)
            throw new IllegalArgumentException("单价不能为空");

        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.receivedQty = 0;
        this.receivedSnCodes = new ArrayList<>();
    }

    /**
     * 收货：登记一个 SN。
     * 包级私有 — 只能通过 PurchaseOrder.receiveItem() 调用。
     */
    void receiveSn(String snCode) {
        if (receivedQty >= quantity) {
            throw new IllegalStateException(
                    "SKU [" + skuId + "] 已收齐，计划 " + quantity + "，已收 " + receivedQty);
        }
        this.receivedSnCodes.add(snCode);
        this.receivedQty++;
    }

    /**
     * 是否已全部收货。
     */
    boolean isFullyReceived() {
        return receivedQty >= quantity;
    }

    // ========== Getters ==========
    public Long getSkuId() {
        return skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Integer getReceivedQty() {
        return receivedQty;
    }

    public List<String> getReceivedSnCodes() {
        return Collections.unmodifiableList(receivedSnCodes);
    }
}
