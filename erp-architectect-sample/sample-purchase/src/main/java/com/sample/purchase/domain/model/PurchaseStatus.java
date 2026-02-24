package com.sample.purchase.domain.model;

/**
 * === 设计要点 ===
 * PurchaseStatus 枚举 — L2 级别的状态管理。
 *
 * 对比 L3 的 InventoryStatus：
 * - L3 在枚举内部定义了 allowedTransitions()，状态"知道"自己能去哪
 * - L2 只是简单枚举，状态校验在聚合根的 if 判断中
 *
 * 为什么 L2 不需要状态转换表？
 * - 采购订单的状态流转比较线性（草稿→提交→审核→收货→完成）
 * - 分支少（只有审核驳回一个回退路径）
 * - 用 if 判断足够清晰，不需要额外的抽象
 */
public enum PurchaseStatus {

    /** 草稿 */
    DRAFT,

    /** 已提交（待审核） */
    SUBMITTED,

    /** 审核通过 */
    APPROVED,

    /** 收货中 */
    RECEIVING,

    /** 已完成 */
    COMPLETED,

    /** 审核驳回 */
    REJECTED,

    /** 已取消 */
    CANCELLED
}
