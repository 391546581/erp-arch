# 务实型 DDD 设计原则 (Pragmatic DDD Principles)

> 本文档定义本项目的架构设计原则。不追求教科书式 DDD，而是选择**对 3C 数码 ERP 场景最有价值的模式**。

---

## 原则 1：按复杂度分级建模

不是所有模块都需要聚合根和状态机。按业务复杂度选择合适的建模深度：

| 级别 | 建模方式 | 适用上下文 | 标志特征 |
| :--- | :--- | :--- | :--- |
| **L3 充血模型** | 聚合根 + 状态机 + 领域事件 + 值对象 | 库存、质检、财务（成本账本） | 有复杂状态转换、业务规则强 |
| **L2 轻量领域** | 实体 + 仓储 + 应用服务编排 | 采购、销售、回收、维保 | 有业务流程但状态较线性 |
| **L1 简单 CRUD** | MyBatis-Plus + Service | 商品（品牌/类目/SPU）、组织、CRM | 主要是配置数据，业务逻辑少 |

### 判断标准
- 如果一个实体的状态转换超过 **5 种**，用充血模型
- 如果需要跨模块联动（触发事件），该实体是聚合根
- 如果只是增删改查 + 简单校验，直接 CRUD

---

## 原则 2：限界上下文是第一公民

**不论建模级别如何，模块间的物理隔离必须严格执行。**

```
✅ 正确：erp2-purchase 通过 PurchaseCompletedEvent 通知 erp2-inventory
❌ 错误：erp2-purchase 直接注入 InventoryRepository 操作库存表
```

### 模块间通信方式

| 方式 | 使用场景 | 实现 |
| :--- | :--- | :--- |
| **领域事件 (Domain Event)** | 状态变更后通知其他模块 | Spring ApplicationEvent（单体阶段） |
| **共享契约 (Published Language)** | 模块间传递数据 | `erp2-api` 中的 DTO/Event 定义 |
| **防腐层 (ACL)** | 对接外部系统（闲鱼/渠道） | Channel 模块内部适配器 |

---

## 原则 3：SN 是贯穿全系统的核心概念

**一机一码一价**是本系统的核心竞争力。以下规则不可妥协：

1. **SN 唯一性**：同一租户下，同一 SN 在非终态（SOLD/SCRAPPED）下只能存在一条记录
2. **个别计价法**：成本核算以 SN 为最小单位，不允许加权平均
3. **全生命周期追踪**：任何改变 SN 状态的操作必须留痕（领域事件）
4. **分布式锁**：SN 状态变更必须使用分布式锁，防止并发冲突

---

## 原则 4：聚合设计准则

### 聚合要小
```
✅ 正确：PurchaseOrder 聚合只包含 PurchaseItem（订单明细）
❌ 错误：PurchaseOrder 聚合包含 Supplier + Warehouse + InventoryItem
```

### 聚合间通过 ID 引用
```
✅ 正确：PurchaseItem.skuId（仅存储 ID）
❌ 错误：PurchaseItem.sku（直接持有 Sku 对象引用）
```

### 一个事务只修改一个聚合
```
✅ 正确：采购收货 → 修改 PurchaseOrder → 发布 PurchaseCompletedEvent → 异步创建 InventoryItem
❌ 错误：采购收货 → 同一事务中修改 PurchaseOrder + 创建 InventoryItem + 记录 CostEntry
```

---

## 原则 5：渐进式演进

### 从单体到微服务的路径
```
阶段 1（当前）：模块化单体 (Modular Monolith)
  - 所有模块在同一个 JVM 中运行
  - 通过 Spring Event 实现模块间通信
  - Maven 多模块保证编译期隔离

阶段 2（按需）：逐步拆分
  - 将高负载模块（如渠道对接）拆为独立服务
  - Spring Event → MQ (RocketMQ/Kafka) 替换
  - 共享数据库 → 独立数据库

阶段 3（远期）：完全微服务
  - 每个限界上下文独立部署
  - API Gateway + 服务发现
```

### 架构迭代节奏
- **每月**：回顾一次上下文边界是否合理
- **每季度**：评估是否需要调整域分类（核心域/支撑域）
- **每次重大业务变更**：更新对应的战术设计文件 + CHANGELOG
