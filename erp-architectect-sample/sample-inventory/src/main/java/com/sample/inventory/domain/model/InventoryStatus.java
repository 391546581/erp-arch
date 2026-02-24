package com.sample.inventory.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * === 设计要点 ===
 * InventoryStatus 枚举 + 状态转换表。
 *
 * 这是 L3 模型的核心差异之一：
 * - L2 模型中的状态只是简单枚举，状态校验写在 if-else 中
 * - L3 模型中，每个状态"知道"自己能转到哪些状态
 *
 * 好处：
 * 1. 状态转换规则集中管理，不会散落在各处
 * 2. 新增状态时，编译器会强制你定义转换规则
 * 3. 非法转换可以给出精确的错误信息
 */
public enum InventoryStatus {

    /** 待质检 — 刚入库，等待质检分配 */
    PENDING_INSPECT,

    /** 质检中 — 正在执行质检 */
    INSPECTING,

    /** 在库可售 — 质检通过，可以上架/锁定 */
    IN_STOCK,

    /** 需维修 — 质检不通过，等待维修 */
    NEED_REPAIR,

    /** 维修中 — 正在维修 */
    REPAIRING,

    /** 已维修 — 维修完成，等待复检 */
    REPAIRED,

    /** 已锁定 — 被销售订单锁定 */
    LOCKED,

    /** 已售出 — 终态 */
    SOLD,

    /** 已报废 — 终态 */
    SCRAPPED;

    /**
     * 状态转换表：定义每个状态允许转换到的目标状态。
     *
     * 这比在聚合根中写 if-else 更安全：
     * - 规则集中在一处
     * - 新增状态忘记定义转换 → 默认不允许转换（安全失败）
     */
    public Set<InventoryStatus> allowedTransitions() {
        return switch (this) {
            case PENDING_INSPECT -> EnumSet.of(INSPECTING);
            case INSPECTING -> EnumSet.of(IN_STOCK, NEED_REPAIR, SCRAPPED);
            case IN_STOCK -> EnumSet.of(LOCKED);
            case NEED_REPAIR -> EnumSet.of(REPAIRING);
            case REPAIRING -> EnumSet.of(REPAIRED);
            case REPAIRED -> EnumSet.of(INSPECTING); // 复检
            case LOCKED -> EnumSet.of(IN_STOCK, SOLD); // 释放 或 出库
            case SOLD, SCRAPPED -> EnumSet.noneOf(InventoryStatus.class); // 终态不可转换
        };
    }

    /**
     * 检查是否可以转换到目标状态。
     */
    public boolean canTransitionTo(InventoryStatus target) {
        return allowedTransitions().contains(target);
    }

    /**
     * 是否为终态（不可再转换）。
     */
    public boolean isTerminal() {
        return this == SOLD || this == SCRAPPED;
    }
}
