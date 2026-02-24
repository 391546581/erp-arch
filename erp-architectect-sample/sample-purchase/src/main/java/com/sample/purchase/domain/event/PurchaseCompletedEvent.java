package com.sample.purchase.domain.event;

import com.sample.common.DomainEvent;
import com.sample.purchase.domain.model.PurchaseItem;
import com.sample.purchase.domain.vo.PurchaseOrderId;

import java.util.List;

/**
 * === 设计要点 ===
 * PurchaseCompletedEvent：采购完成领域事件。
 *
 * 消费者：
 * - 库存中心：为每个 SN 创建库存项 (PENDING_INSPECT)
 * - 财务中心：记录采购成本 (ACQUISITION)
 */
public class PurchaseCompletedEvent extends DomainEvent {

    private final PurchaseOrderId orderId;
    private final Long supplierId;
    private final List<PurchaseItem> items;

    public PurchaseCompletedEvent(
            PurchaseOrderId orderId,
            Long supplierId,
            List<PurchaseItem> items) {
        super(orderId.toString());
        this.orderId = orderId;
        this.supplierId = supplierId;
        this.items = items;
    }

    public PurchaseOrderId getOrderId() {
        return orderId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public List<PurchaseItem> getItems() {
        return items;
    }
}
