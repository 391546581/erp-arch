package com.sample.purchase.application;

import com.sample.common.vo.Money;
import com.sample.purchase.domain.model.PurchaseOrder;
import com.sample.purchase.domain.repository.PurchaseOrderRepository;
import com.sample.purchase.domain.vo.PurchaseOrderId;

/**
 * === 设计要点 ===
 * 采购应用服务 — L2 级别。
 *
 * 对比 L3 的 InventoryApplicationService：
 * - 结构一样（加载 → 调用 → 保存）
 * - L2 的应用服务同样很"薄"
 * - 区别在于聚合根内部的复杂度不同
 *
 * 这说明什么？
 * L2 和 L3 的应用服务写法几乎相同。
 * 真正的差异在 Domain 层（聚合根的复杂度）。
 * 应用服务始终只做 "编排"，不做 "判断"。
 */
public class PurchaseApplicationService {

    private final PurchaseOrderRepository repository;

    public PurchaseApplicationService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建采购单草稿。
     */
    public PurchaseOrderId createDraft(Long supplierId, String remark) {
        PurchaseOrder order = PurchaseOrder.createDraft(supplierId, remark);
        repository.save(order);
        return order.getOrderId();
    }

    /**
     * 添加采购明细。
     */
    public void addItem(Long orderId, Long skuId, Integer quantity, String unitPriceStr) {
        PurchaseOrder order = loadOrder(orderId);

        Money unitPrice = Money.of(unitPriceStr);
        order.addItem(skuId, quantity, unitPrice);

        repository.save(order);
    }

    /**
     * 提交审核。
     */
    public void submit(Long orderId) {
        PurchaseOrder order = loadOrder(orderId);
        order.submit();
        repository.save(order);
    }

    /**
     * 审核通过。
     */
    public void approve(Long orderId) {
        PurchaseOrder order = loadOrder(orderId);
        order.approve();
        repository.save(order);
    }

    /**
     * 收货。
     */
    public void receiveItem(Long orderId, Long skuId, String snCode) {
        PurchaseOrder order = loadOrder(orderId);
        order.receiveItem(skuId, snCode);
        repository.save(order);
    }

    /**
     * 完成。
     */
    public void complete(Long orderId) {
        PurchaseOrder order = loadOrder(orderId);
        order.complete();
        repository.save(order);

        // 发布事件
        // order.getDomainEvents().forEach(eventPublisher::publish);
        // order.clearDomainEvents();
    }

    // ========== 私有方法 ==========

    private PurchaseOrder loadOrder(Long orderId) {
        return repository.findById(PurchaseOrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("采购订单不存在: " + orderId));
    }
}
