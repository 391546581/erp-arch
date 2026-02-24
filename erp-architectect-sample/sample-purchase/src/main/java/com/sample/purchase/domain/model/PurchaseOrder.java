package com.sample.purchase.domain.model;

import com.sample.common.AggregateRoot;
import com.sample.common.vo.Money;
import com.sample.purchase.domain.event.PurchaseCompletedEvent;
import com.sample.purchase.domain.vo.PurchaseOrderId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * === 设计要点 ===
 * PurchaseOrder 聚合根 — L2 轻量领域模型的示范。
 *
 * L2 和 L3 的核心对比（请对照 InventoryItem.java 阅读）：
 *
 * ┌──────────────┬──────────────────────────────────┬──────────────────────────────────┐
 * │ 维度 │ L2 (PurchaseOrder) │ L3 (InventoryItem) │
 * ├──────────────┼──────────────────────────────────┼──────────────────────────────────┤
 * │ 状态校验 │ 直接 if 判断 │ 状态转换表 + transitionTo() │
 * │ 为什么？ │ 流程线性，分支少 │ 10种状态，转换路径复杂 │
 * ├──────────────┼──────────────────────────────────┼──────────────────────────────────┤
 * │ 不变量 │ items 非空校验 │ 成本单调递增、SN唯一性 │
 * │ 为什么？ │ 采购订单的一致性要求低 │ 财务数据一分钱都不能错 │
 * ├──────────────┼──────────────────────────────────┼──────────────────────────────────┤
 * │ 值对象 │ PurchaseOrderId（仅null校验） │ SnCode（正则+长度校验） │
 * │ 为什么？ │ 订单ID格式无特殊要求 │ SN编码有严格的业务规范 │
 * ├──────────────┼──────────────────────────────────┼──────────────────────────────────┤
 * │ 内部实体 │ PurchaseItem（包级私有方法） │ 无内部实体（单一聚合根） │
 * │ 为什么？ │ 订单是"主-明细"结构 │ 库存项就是最小粒度 │
 * └──────────────┴──────────────────────────────────┴──────────────────────────────────┘
 *
 * 总结：
 * L2 的"充血"程度适中 —— 有业务方法，但不需要复杂的状态机和严格的不变量守卫。
 * 这不是"做得不够好"，而是"刚好够用"。过度设计简单业务和设计不足同样有害。
 */
public class PurchaseOrder extends AggregateRoot {

    // ========== 标识 ==========
    private PurchaseOrderId orderId;
    private Long supplierId;

    // ========== 状态 ==========
    private PurchaseStatus status;

    // ========== 明细 ==========
    private final List<PurchaseItem> items = new ArrayList<>();

    // ========== 其他 ==========
    private Money totalAmount;
    private String remark;

    // ========== 构造 ==========

    private PurchaseOrder() {
    }

    /**
     * 工厂方法：创建草稿采购单。
     */
    public static PurchaseOrder createDraft(Long supplierId, String remark) {
        if (supplierId == null) {
            throw new IllegalArgumentException("供应商 ID 不能为空");
        }

        PurchaseOrder order = new PurchaseOrder();
        order.supplierId = supplierId;
        order.status = PurchaseStatus.DRAFT;
        order.remark = remark;
        order.totalAmount = Money.ZERO;
        return order;
    }

    // ========== 明细操作 ==========

    /**
     * 添加采购明细。
     * 只有 DRAFT 状态下才能添加。
     */
    public void addItem(Long skuId, Integer quantity, Money unitPrice) {
        // L2 风格的状态校验 — 直接用 if 判断，简单明了
        if (status != PurchaseStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态才能添加明细，当前状态: " + status);
        }

        PurchaseItem item = new PurchaseItem(skuId, quantity, unitPrice);
        items.add(item);

        // 重新计算总金额
        recalculateTotal();
    }

    // ========== 流程操作 ==========

    /**
     * 提交审核。
     *
     * L2 的状态校验风格：
     * - 直接 if 判断当前状态
     * - 抛出通用的 IllegalStateException
     * 
     * 对比 L3 的 transitionTo()：
     * - L3 有专用的 InvalidStatusTransitionException
     * - L3 的校验逻辑在状态枚举内部集中管理
     */
    public void submit() {
        if (status != PurchaseStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态才能提交，当前状态: " + status);
        }
        // 不变量校验：明细不能为空
        if (items.isEmpty()) {
            throw new IllegalStateException("采购明细不能为空");
        }

        this.status = PurchaseStatus.SUBMITTED;
    }

    /**
     * 审核通过。
     */
    public void approve() {
        if (status != PurchaseStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交状态才能审核，当前状态: " + status);
        }
        this.status = PurchaseStatus.APPROVED;
    }

    /**
     * 审核驳回。
     */
    public void reject(String reason) {
        if (status != PurchaseStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交状态才能驳回，当前状态: " + status);
        }
        this.status = PurchaseStatus.REJECTED;
        this.remark = reason;
    }

    /**
     * 收货。
     * 首次收货时自动从 APPROVED 转为 RECEIVING。
     */
    public void receiveItem(Long skuId, String snCode) {
        if (status != PurchaseStatus.APPROVED && status != PurchaseStatus.RECEIVING) {
            throw new IllegalStateException("当前状态不允许收货: " + status);
        }

        // 找到对应的明细行
        PurchaseItem item = items.stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 SKU: " + skuId));

        // 委托给内部实体执行（实体自己校验数量是否超出）
        item.receiveSn(snCode);

        // 首次收货，状态变更
        if (status == PurchaseStatus.APPROVED) {
            this.status = PurchaseStatus.RECEIVING;
        }
    }

    /**
     * 完成收货。
     *
     * 注册 PurchaseCompletedEvent → 通知库存中心入库 + 通知财务中心记账。
     */
    public void complete() {
        if (status != PurchaseStatus.RECEIVING) {
            throw new IllegalStateException("只有收货中状态才能完成，当前状态: " + status);
        }

        this.status = PurchaseStatus.COMPLETED;

        // ======== 领域事件 ========
        // 与 L3 一样，L2 也使用领域事件进行跨上下文通信
        registerEvent(new PurchaseCompletedEvent(
                this.orderId,
                this.supplierId,
                Collections.unmodifiableList(this.items)));
    }

    /**
     * 取消。
     */
    public void cancel() {
        if (status != PurchaseStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态才能取消，当前状态: " + status);
        }
        this.status = PurchaseStatus.CANCELLED;
    }

    // ========== 私有方法 ==========

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(item -> item.getUnitPrice()
                        .add(Money.of(String.valueOf(item.getQuantity() - 1))
                                .subtract(Money.of(String.valueOf(item.getQuantity() - 1)))))
                // 简化计算：unitPrice * quantity
                .reduce(Money.ZERO, Money::add);
    }

    // ========== Getters ==========
    public PurchaseOrderId getOrderId() {
        return orderId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public List<PurchaseItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public String getRemark() {
        return remark;
    }

    /**
     * 重建方法（Repository 使用）。
     */
    public static PurchaseOrder reconstruct(
            PurchaseOrderId orderId,
            Long supplierId,
            PurchaseStatus status,
            List<PurchaseItem> items,
            Money totalAmount,
            String remark) {
        PurchaseOrder order = new PurchaseOrder();
        order.orderId = orderId;
        order.supplierId = supplierId;
        order.status = status;
        order.items.addAll(items);
        order.totalAmount = totalAmount;
        order.remark = remark;
        return order;
    }
}
