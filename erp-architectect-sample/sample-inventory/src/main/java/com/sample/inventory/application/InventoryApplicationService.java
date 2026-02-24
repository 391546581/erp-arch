package com.sample.inventory.application;

import com.sample.common.DomainEvent;
import com.sample.common.vo.Money;
import com.sample.inventory.domain.model.InventoryItem;
import com.sample.inventory.domain.model.SourceType;
import com.sample.inventory.domain.repository.InventoryItemRepository;
import com.sample.inventory.domain.vo.InventoryItemId;
import com.sample.inventory.domain.vo.SnCode;
import com.sample.inventory.domain.vo.WarehouseId;

/**
 * === 设计要点 ===
 * InventoryApplicationService：应用服务层。
 *
 * 应用服务是 L2 和 L3 都有的，但职责始终不变：
 * 1.【编排】调用聚合根的业务方法
 * 2.【事务】控制事务边界
 * 3.【事件】在事务提交后发布领域事件
 * 4.【查询】组合数据返回给调用方
 *
 * ⚠️ 关键原则：应用服务中 **绝不包含业务逻辑判断**！
 * - ✅ "调用 item.lockForOrder(orderId)" — 正确，编排
 * - ❌ "if (item.getStatus() == IN_STOCK) { item.setStatus(LOCKED); }" —
 * 错误，业务逻辑泄露到应用层
 *
 * 对比：
 * - 在传统 CRUD (L1) 架构中，所有逻辑都在 Service 中
 * - 在 L2/L3 中，Service 变"薄"了，逻辑在聚合根内部
 * - L3 的 Service 更薄，因为聚合根承担了更多的校验和状态管理
 */
public class InventoryApplicationService {

    private final InventoryItemRepository repository;
    // 实际项目中还需要 EventPublisher、DistributedLock 等
    // private final DomainEventPublisher eventPublisher;
    // private final DistributedLock distributedLock;

    public InventoryApplicationService(InventoryItemRepository repository) {
        this.repository = repository;
    }

    /**
     * 用例：SN 入库。
     *
     * 注意应用服务的职责边界：
     * 1. 前置校验（SN 唯一性，跨聚合的校验放这里）
     * 2. 调用聚合根工厂方法创建
     * 3. 持久化
     * 4. 发布事件
     *
     * 业务逻辑（如成本初始化、状态设置）全在聚合根内部。
     */
    public InventoryItemId stockIn(
            String snCodeStr,
            Long skuId,
            Long warehouseId,
            String acquisitionCostStr,
            SourceType sourceType,
            String sourceOrderId) {
        // 1. 构建值对象（值对象会自校验格式）
        SnCode snCode = SnCode.of(snCodeStr);
        WarehouseId whId = WarehouseId.of(warehouseId);
        Money cost = Money.of(acquisitionCostStr);

        // 2. 跨聚合的业务校验（SN 唯一性）
        // 这个校验不适合放在聚合根内部，因为它需要查询数据库
        if (repository.existsActiveBySnCode(snCode)) {
            throw new IllegalStateException("SN [" + snCode + "] 已存在活跃库存记录，不允许重复入库");
        }

        // 3. 调用聚合根的工厂方法（业务逻辑在聚合根内部）
        InventoryItem item = InventoryItem.stockIn(snCode, skuId, whId, cost, sourceType, sourceOrderId);

        // 4. 持久化
        repository.save(item);

        // 5. 发布事件（实际项目中通过 EventPublisher）
        // item.getDomainEvents().forEach(eventPublisher::publish);
        // item.clearDomainEvents();

        return item.getItemId();
    }

    /**
     * 用例：质检通过。
     *
     * 看看这个方法有多"薄" ——
     * 应用服务只做了3件事：查询、调用、保存。
     * 所有状态校验逻辑都在 InventoryItem.passInspection() 内部。
     */
    public void passInspection(Long itemId) {
        // 1. 加载聚合根
        InventoryItem item = repository.findById(InventoryItemId.of(itemId))
                .orElseThrow(() -> new IllegalArgumentException("库存项不存在: " + itemId));

        // 2. 调用业务方法（所有状态校验在聚合根内部）
        item.passInspection();

        // 3. 持久化
        repository.save(item);
    }

    /**
     * 用例：订单锁定库存。
     */
    public void lockForOrder(Long itemId, String orderId) {
        InventoryItem item = repository.findById(InventoryItemId.of(itemId))
                .orElseThrow(() -> new IllegalArgumentException("库存项不存在: " + itemId));

        // 实际项目中这里应该加分布式锁
        // distributedLock.lock("inventory:sn:" + item.getSnCode().getValue());

        item.lockForOrder(orderId);

        repository.save(item);
    }

    /**
     * 用例：出库确认。
     *
     * 注意事件的处理流程：
     * 1. 聚合根的 confirmStockOut() 内部注册了 StockOutEvent
     * 2. 应用服务在持久化后取出事件并发布
     * 3. 财务中心监听 StockOutEvent 执行成本结转
     */
    public void confirmStockOut(Long itemId) {
        InventoryItem item = repository.findById(InventoryItemId.of(itemId))
                .orElseThrow(() -> new IllegalArgumentException("库存项不存在: " + itemId));

        item.confirmStockOut();

        repository.save(item);

        // 发布领域事件
        // item.getDomainEvents().forEach(eventPublisher::publish);
        // item.clearDomainEvents();
    }

    /**
     * 用例：维修完成。
     */
    public void completeRepair(Long itemId, String repairCostStr) {
        InventoryItem item = repository.findById(InventoryItemId.of(itemId))
                .orElseThrow(() -> new IllegalArgumentException("库存项不存在: " + itemId));

        Money repairCost = Money.of(repairCostStr);
        item.completeRepair(repairCost);

        repository.save(item);
    }
}
