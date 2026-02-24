# 限界上下文映射图 (Context Map)

> 本图展示 fprice-erp-saas 系统的所有限界上下文及其关系。

## 全局上下文映射

```mermaid
graph TB
    subgraph 通用域["🔧 通用域 (Generic)"]
        Product["📱 商品中心<br/>erp2-product<br/>品牌/类目/SPU/SKU"]
        Org["🏛️ 组织中心<br/>erp2-org<br/>租户/门店/权限"]
        CRM["👥 客户中心<br/>erp2-crm<br/>客户/供应商"]
    end

    subgraph 核心域["⭐ 核心域 (Core)"]
        Inventory["📦 库存中心<br/>erp2-inventory<br/>SN管理/仓位/状态"]
        Quality["🔍 质检中心<br/>erp2-quality<br/>模板/报告/评级"]
    end

    subgraph 核心支撑域["🔗 核心支撑域 (Core-Supporting)"]
        Purchase["🛒 采购中心<br/>erp2-purchase<br/>计划/订单/退货"]
        Sale["💰 销售中心<br/>erp2-sale<br/>订单/售后/竞拍"]
        Recovery["♻️ 回收中心<br/>erp2-recovery<br/>估价/回收/分发"]
        Service["🔧 维保中心<br/>erp2-service<br/>工单/配件/物流"]
    end

    subgraph 支撑域["📎 支撑域 (Supporting)"]
        Channel["🏢 渠道中心<br/>erp2-channel<br/>闲鱼/渠道对接"]
    end

    subgraph 价值域["💎 价值域 (Value)"]
        Finance["💳 财务中心<br/>erp2-finance<br/>成本/账户/结算"]
    end

    subgraph 基础设施["⚙️ 基础设施"]
        API["erp2-api<br/>共享契约/DTO/Event"]
        Common["erp2-common<br/>工具类/异常/Base"]
    end

    %% === 上下游关系 ===
    %% 采购 → 库存（采购完成触发入库）
    Purchase -->|"PurchaseCompletedEvent"| Inventory
    
    %% 回收 → 库存（回收确认触发入库）
    Recovery -->|"RecoveryPaidEvent"| Inventory
    
    %% 质检 ↔ 库存（质检结果更新库存状态）
    Quality -->|"InspectionCompletedEvent"| Inventory
    
    %% 维保 → 库存 + 财务（维修完成更新状态和成本）
    Service -->|"RepairCompletedEvent"| Inventory
    Service -->|"RepairCompletedEvent"| Finance

    %% 库存 → 财务（出库触发成本结转）
    Inventory -->|"StockOutEvent"| Finance

    %% 销售 → 库存（锁库/出库）
    Sale -->|"锁库/扣库存"| Inventory
    Sale -->|"SaleCompletedEvent"| Finance
    
    %% 渠道 → 销售（渠道订单同步）
    Channel -->|"ChannelOrderSyncEvent"| Sale
    
    %% 采购 → 财务（应付账款）
    Purchase -->|"PurchaseCompletedEvent"| Finance
    
    %% 回收 → 财务（回收应付）
    Recovery -->|"RecoveryPaidEvent"| Finance

    %% 所有域引用商品
    Purchase -.->|"SKU引用"| Product
    Quality -.->|"模板关联类目"| Product
    Inventory -.->|"商品引用"| Product
    Recovery -.->|"机型引用"| Product
    Sale -.->|"商品引用"| Product
    Channel -.->|"渠道映射"| Product
    
    %% 所有域依赖基础设施
    Product --> API
    Purchase --> API
    Quality --> API
    Inventory --> API
    Service --> API
    Recovery --> API
    Sale --> API
    Finance --> API
    Channel --> API
```

## 上下文关系类型说明

| 关系 | 上游 (Upstream) | 下游 (Downstream) | 模式 |
| :--- | :--- | :--- | :--- |
| 采购→库存 | Purchase | Inventory | **Customer-Supplier** (事件驱动) |
| 回收→库存 | Recovery | Inventory | **Customer-Supplier** (事件驱动) |
| 质检→库存 | Quality | Inventory | **Customer-Supplier** (事件驱动) |
| 维保→库存 | Service | Inventory | **Customer-Supplier** (事件驱动) |
| 库存→财务 | Inventory | Finance | **Customer-Supplier** (事件驱动) |
| 渠道→销售 | Channel | Sale | **防腐层 (ACL)** — 外部数据格式适配 |
| 所有→商品 | Product | 所有核心域 | **Published Language** (共享 SKU/SPU 定义) |
| 所有→API | erp2-api | 所有模块 | **Shared Kernel** (共享契约) |

## 迭代记录

| 日期 | 变更 | 原因 |
| :--- | :--- | :--- |
| 2026-02-24 | 初始版本 | 基于 erp-model/ARCHITECTURE.md 梳理 |
