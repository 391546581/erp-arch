# DDD 模型演示代码 — L2 vs L3 对比

> 通过两个完整的领域建模示例，直观展示 L2 (轻量领域) 和 L3 (充血模型) 的关键差异。

## 快速导航

| 模块 | 级别 | 核心看点 | 入口 |
| :--- | :--- | :--- | :--- |
| **sample-inventory** | L3 充血模型 | 状态机、值对象自校验、不变量守卫 | [InventoryItem.java](./sample-inventory/src/main/java/com/sample/inventory/domain/model/InventoryItem.java) |
| **sample-purchase** | L2 轻量领域 | 简洁的聚合根、实体保护、事件发布 | [PurchaseOrder.java](./sample-purchase/src/main/java/com/sample/purchase/domain/model/PurchaseOrder.java) |

## L2 vs L3 关键差异对照

| 维度 | L2 采购中心 | L3 库存中心 |
| :--- | :--- | :--- |
| **状态管理** | 枚举 + `if` 校验 | 显式状态转换表 + 守卫方法 |
| **值对象** | ID 封装 (`PurchaseOrderId`) | ID + 业务 VO (`SnCode` 含格式校验, `Money` 含运算) |
| **不变量** | 简单校验 (明细不为空) | 多重守卫 (SN 唯一性、成本单调递增、状态互斥) |
| **领域事件** | 在 `complete()` 时发布 | 发布 + 消费多个事件 |
| **领域异常** | 通用 `IllegalStateException` | 专用 `InvalidStatusTransitionException` |
| **领域服务** | 无 | 有 (跨聚合的库存入库编排) |
| **聚合根方法数** | 5-6 个 | 10+ 个 |

## 阅读建议

1. **先看 `sample-common`**：理解 `AggregateRoot` 基类如何管理领域事件
2. **再看 `sample-purchase` (L2)**：理解基本的 DDD 分层结构
3. **最后看 `sample-inventory` (L3)**：对比 L2，理解"充血"在哪里
4. **重点关注**：每个文件顶部的 `/* === 设计要点 === */` 注释块





DDD 架构即代码 + 模型演示 — 完成总结
一、架构设计框架 (erp-architectect/, 21 files)
建立了可迭代的 DDD 架构设计体系，包含战略设计、11 个限界上下文的战术设计、业务流程图和迭代工具。

📖 入口：
README.md

二、L2/L3 模型演示代码 (erp-architectect-sample/, 21 files)
推荐阅读顺序
AggregateRoot.java
 — 事件收集机制
Money.java
 — 值对象典范（不可变、自校验）
PurchaseOrder.java
 — L2 聚合根（先看简单的）
InventoryStatus.java
 — L3 状态转换表（核心差异点）
InventoryItem.java
 — L3 聚合根（最重要的文件）
InventoryApplicationService.java
 — 看看应用服务有多"薄"
L2 vs L3 一句话总结
L2 和 L3 的应用服务写法几乎相同（加载→调用→保存）。真正的差异在聚合根内部：L3 用状态转换表集中管理规则，L2 用 if 判断即可。