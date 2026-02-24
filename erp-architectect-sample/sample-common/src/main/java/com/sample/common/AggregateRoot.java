package com.sample.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * === 设计要点 ===
 * 聚合根基类：提供领域事件的注册与收集机制。
 *
 * 核心思路：
 * 1. 聚合根内部的业务方法在执行完逻辑后，调用 registerEvent() 注册事件
 * 2. 事件不会立即发送，而是先暂存在聚合根内部
 * 3. 应用服务（Application Service）在提交事务后，统一取出事件并发布
 * 4. 这保证了"先持久化，后发事件"的一致性
 */
public abstract class AggregateRoot {

    /** 暂存的领域事件列表 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 注册一个领域事件（由聚合根的业务方法调用）。
     * 事件不会立即发送，而是等应用服务统一处理。
     */
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * 获取所有待发布的领域事件（由应用服务调用）。
     * 返回不可变列表，防止外部篡改。
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 清空已发布的事件（由应用服务在发布后调用）。
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
