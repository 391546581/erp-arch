package com.sample.common;

import java.time.LocalDateTime;

/**
 * === 设计要点 ===
 * 领域事件基类：所有领域事件的公共属性。
 *
 * 领域事件的命名约定：
 * - 使用过去式（如 PurchaseCompleted，不是 CompletePurchase）
 * - 表示"已经发生的事实"，不是"请求"或"命令"
 */
public abstract class DomainEvent {

    /** 事件发生时间 */
    private final LocalDateTime occurredAt;

    /** 事件来源聚合根的标识 */
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this.aggregateId = aggregateId;
        this.occurredAt = LocalDateTime.now();
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }
}
