package com.sample.inventory.domain.repository;

import com.sample.inventory.domain.model.InventoryItem;
import com.sample.inventory.domain.vo.InventoryItemId;
import com.sample.inventory.domain.vo.SnCode;

import java.util.Optional;

/**
 * === 设计要点 ===
 * Repository 接口：定义在 Domain 层，实现在 Infrastructure 层。
 *
 * 这是 DDD 依赖倒置的关键：
 * - Domain 层只定义"我需要什么能力"（接口）
 * - Infrastructure 层负责"怎么实现"（MyBatis、JPA 等）
 * - Domain 层不依赖任何框架
 *
 * 接口设计原则：
 * 1. 以聚合根为单位存取（不能直接操作内部实体）
 * 2. 方法命名使用业务语言（findBySnCode 而非 selectBySnCode）
 * 3. 返回 Optional 而非 null
 */
public interface InventoryItemRepository {

    /**
     * 保存库存项（新增或更新）。
     */
    void save(InventoryItem item);

    /**
     * 根据 ID 查找。
     */
    Optional<InventoryItem> findById(InventoryItemId id);

    /**
     * 根据 SN 查找当前活跃的库存项（非终态）。
     * 业务规则：同一租户同一 SN 只能有一条非终态记录。
     */
    Optional<InventoryItem> findActiveBySnCode(SnCode snCode);

    /**
     * 检查 SN 是否已存在活跃记录。
     * 用于入库前的唯一性检查。
     */
    boolean existsActiveBySnCode(SnCode snCode);
}
