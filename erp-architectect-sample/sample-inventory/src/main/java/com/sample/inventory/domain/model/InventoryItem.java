package com.sample.inventory.domain.model;

import com.sample.common.AggregateRoot;
import com.sample.common.vo.Money;
import com.sample.inventory.domain.event.StockOutEvent;
import com.sample.inventory.domain.exception.InvalidStatusTransitionException;
import com.sample.inventory.domain.vo.InventoryItemId;
import com.sample.inventory.domain.vo.SnCode;
import com.sample.inventory.domain.vo.WarehouseId;

/**
 * === 设计要点 ===
 * InventoryItem 聚合根 — L3 充血模型的核心示范。
 *
 * 这是整个示例项目中最重要的一个类。它展示了 L3 模型的所有关键特征：
 *
 * 1.【状态机】所有状态变更通过专用方法执行，内部自动校验合法性
 * - 外部不能直接 setStatus()，必须调用 passInspection() / lockForOrder() 等
 * - 非法转换抛出 InvalidStatusTransitionException
 *
 * 2.【不变量保护】聚合根保证内部状态永远一致
 * - accumulatedCost = acquisitionCost + Σ repairCosts（automatically maintained）
 * - LOCKED 状态下不允许被其他订单锁定
 *
 * 3.【领域事件】业务动作完成后注册事件，不直接调用其他模块
 * - confirmStockOut() 注册 StockOutEvent → 由应用服务发布 → 通知财务中心
 *
 * 4.【封装性】外部只能通过业务方法操作，不能直接修改内部状态
 * - 没有 setStatus()、setAccumulatedCost() 等方法
 * - 所有修改都带有业务校验
 *
 * 对比 L2 的 PurchaseOrder：
 * - PurchaseOrder 也有业务方法，但状态校验更简单（if 判断）
 * - PurchaseOrder 不需要"成本累加"这样的复杂不变量
 * - PurchaseOrder 的 item 操作相对直观（添加/接收）
 */
public class InventoryItem extends AggregateRoot {

    // ========== 标识 ==========
    private InventoryItemId itemId;
    private SnCode snCode;
    private Long skuId;

    // ========== 位置 ==========
    private WarehouseId warehouseId;

    // ========== 状态 ==========
    private InventoryStatus status;

    // ========== 成本 ==========
    /** 获取成本（采购价或回收价），入库时确定，不可变 */
    private Money acquisitionCost;

    /** 累计总成本 = 获取成本 + Σ 维修成本。这是不变量的核心。 */
    private Money accumulatedCost;

    // ========== 来源 ==========
    private SourceType sourceType;
    private String sourceOrderId;

    // ========== 锁定信息 ==========
    /** 锁定这个 SN 的销售订单 ID */
    private String lockedByOrderId;

    // ========== 私有构造 + 工厂方法 ==========

    /**
     * 私有构造，外部只能通过 stockIn() 工厂方法创建。
     * 这确保了"入库"是创建库存项的唯一合法入口。
     */
    private InventoryItem() {
    }

    /**
     * 工厂方法：入库。
     * 这是创建 InventoryItem 的唯一方式。
     * 
     * 为什么用工厂方法而非构造函数？
     * 1. 语义更明确：stockIn() 比 new InventoryItem() 更表达业务意图
     * 2. 可以包含业务校验逻辑
     * 3. 可以自动设置初始状态
     */
    public static InventoryItem stockIn(
            SnCode snCode,
            Long skuId,
            WarehouseId warehouseId,
            Money acquisitionCost,
            SourceType sourceType,
            String sourceOrderId) {
        // ---- 前置校验 ----
        if (snCode == null)
            throw new IllegalArgumentException("SN 编码不能为空");
        if (skuId == null)
            throw new IllegalArgumentException("SKU ID 不能为空");
        if (warehouseId == null)
            throw new IllegalArgumentException("仓库 ID 不能为空");
        if (acquisitionCost == null || !acquisitionCost.isGreaterThan(Money.ZERO)) {
            throw new IllegalArgumentException("获取成本必须大于零");
        }

        // ---- 创建并初始化 ----
        InventoryItem item = new InventoryItem();
        item.snCode = snCode;
        item.skuId = skuId;
        item.warehouseId = warehouseId;
        item.acquisitionCost = acquisitionCost;
        item.accumulatedCost = acquisitionCost; // 初始累计成本 = 获取成本
        item.sourceType = sourceType;
        item.sourceOrderId = sourceOrderId;
        item.status = InventoryStatus.PENDING_INSPECT; // 固定初始状态

        return item;
    }

    // ========== 状态转换方法（这是 L3 的精髓） ==========

    /**
     * 开始质检。
     * PENDING_INSPECT → INSPECTING
     * REPAIRED → INSPECTING（复检）
     */
    public void startInspection() {
        transitionTo(InventoryStatus.INSPECTING);
    }

    /**
     * 质检通过 → 良品入库。
     * INSPECTING → IN_STOCK
     */
    public void passInspection() {
        transitionTo(InventoryStatus.IN_STOCK);
    }

    /**
     * 质检不通过 → 需维修。
     * INSPECTING → NEED_REPAIR
     */
    public void failInspection() {
        transitionTo(InventoryStatus.NEED_REPAIR);
    }

    /**
     * 开始维修。
     * NEED_REPAIR → REPAIRING
     */
    public void startRepair() {
        transitionTo(InventoryStatus.REPAIRING);
    }

    /**
     * 维修完成。
     * REPAIRING → REPAIRED
     *
     * 注意：这里维修成本的累加是不变量保护的示范。
     * accumulatedCost 只能增加不能减少。
     */
    public void completeRepair(Money repairCost) {
        if (repairCost == null || !repairCost.isGreaterThan(Money.ZERO)) {
            throw new IllegalArgumentException("维修成本必须大于零");
        }

        transitionTo(InventoryStatus.REPAIRED);

        // 不变量维护：累加维修成本
        this.accumulatedCost = this.accumulatedCost.add(repairCost);
    }

    /**
     * 订单锁定。
     * IN_STOCK → LOCKED
     *
     * 不变量：已被锁定的库存不能再被其他订单锁定。
     */
    public void lockForOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("订单 ID 不能为空");
        }

        transitionTo(InventoryStatus.LOCKED);
        this.lockedByOrderId = orderId;
    }

    /**
     * 释放锁定（订单取消）。
     * LOCKED → IN_STOCK
     */
    public void releaseLock() {
        transitionTo(InventoryStatus.IN_STOCK);
        this.lockedByOrderId = null;
    }

    /**
     * 出库确认。
     * LOCKED → SOLD
     *
     * 这里演示了领域事件的注册：
     * 1. 状态转换成功
     * 2. 注册 StockOutEvent（携带累计成本，用于财务结转）
     * 3. 应用服务在事务提交后统一发布事件
     */
    public void confirmStockOut() {
        transitionTo(InventoryStatus.SOLD);

        // 注册领域事件 → 通知财务中心进行成本结转
        registerEvent(new StockOutEvent(
                this.snCode,
                this.lockedByOrderId,
                this.accumulatedCost));
    }

    /**
     * 报废。
     * INSPECTING → SCRAPPED
     */
    public void scrap(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("报废原因不能为空");
        }
        transitionTo(InventoryStatus.SCRAPPED);
    }

    // ========== 核心：状态转换引擎 ==========

    /**
     * 统一的状态转换方法。
     *
     * 这是状态机的核心：
     * 1. 检查当前状态是否允许转换到目标状态
     * 2. 不允许 → 抛出专用异常（而非通用 IllegalStateException）
     * 3. 允许 → 执行转换
     *
     * 所有状态转换都经过这个方法，确保没有"后门"可以绕过校验。
     */
    private void transitionTo(InventoryStatus targetStatus) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(this.snCode, this.status, targetStatus);
        }
        this.status = targetStatus;
    }

    // ========== 查询方法（只读，不修改状态） ===========

    public InventoryItemId getItemId() {
        return itemId;
    }

    public SnCode getSnCode() {
        return snCode;
    }

    public Long getSkuId() {
        return skuId;
    }

    public WarehouseId getWarehouseId() {
        return warehouseId;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public Money getAcquisitionCost() {
        return acquisitionCost;
    }

    public Money getAccumulatedCost() {
        return accumulatedCost;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceOrderId() {
        return sourceOrderId;
    }

    public String getLockedByOrderId() {
        return lockedByOrderId;
    }

    /**
     * 用于从数据库重建聚合根（Repository 实现中使用）。
     * 绕过业务校验，直接恢复状态。
     */
    public static InventoryItem reconstruct(
            InventoryItemId itemId,
            SnCode snCode,
            Long skuId,
            WarehouseId warehouseId,
            InventoryStatus status,
            Money acquisitionCost,
            Money accumulatedCost,
            SourceType sourceType,
            String sourceOrderId,
            String lockedByOrderId) {
        InventoryItem item = new InventoryItem();
        item.itemId = itemId;
        item.snCode = snCode;
        item.skuId = skuId;
        item.warehouseId = warehouseId;
        item.status = status;
        item.acquisitionCost = acquisitionCost;
        item.accumulatedCost = accumulatedCost;
        item.sourceType = sourceType;
        item.sourceOrderId = sourceOrderId;
        item.lockedByOrderId = lockedByOrderId;
        return item;
    }
}
