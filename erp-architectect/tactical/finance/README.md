# 💳 财务中心 (Finance Context)

> **分类**：💎 价值域 | **建模级别**：L3 充血模型
> 
> 基于个别计价法，以 SN 为最小单位独立核算成本。承载财务数据的严谨性。

## 职责边界

- ✅ 以 SN 为维度的成本归集与核算
- ✅ 供应商/客户的财务账户与流水管理
- ✅ 周期性结算单的创建与确认
- ❌ 不直接操作库存状态
- ❌ 不直接操作采购/销售订单

## 聚合设计

```mermaid
classDiagram
    class CostLedger {
        <<聚合根>>
        -SnCode snCode
        -List~CostEntry~ entries
        -Money totalCost
        +recordCost(type, amount, sourceOrderId)
        +settleOut() Money
    }

    class CostEntry {
        <<实体>>
        -CostType type
        -Money amount
        -String sourceOrderId
        -LocalDateTime occurredAt
    }

    class CostType {
        <<枚举>>
        ACQUISITION
        REPAIR_PARTS
        REPAIR_LABOR
        LOGISTICS
    }

    class FinanceAccount {
        <<聚合根>>
        -AccountId accountId
        -OwnerType ownerType
        -String ownerId
        -Money balance
        -List~AccountFlow~ flows
        +credit(amount, reason, orderId)
        +debit(amount, reason, orderId)
    }

    class AccountFlow {
        <<实体>>
        -FlowType flowType
        -Money amount
        -String reason
        -String orderId
        -LocalDateTime occurredAt
    }

    class Settlement {
        <<聚合根>>
        -SettlementId settlementId
        -String partnerId
        -SettlePeriod periodConfig
        -List~SettlementItem~ items
        -Money totalPayable
        -Money totalReceivable
        -SettlementStatus status
        +addItem(item)
        +confirm()
        +markPaid()
    }

    class SettlementStatus {
        <<枚举>>
        DRAFT
        CONFIRMED
        PAID
    }

    CostLedger "1" *-- "*" CostEntry
    CostEntry --> CostType
    FinanceAccount "1" *-- "*" AccountFlow
    Settlement --> SettlementStatus
```

## 成本归集流程

```mermaid
sequenceDiagram
    participant P as 采购中心
    participant R as 回收中心
    participant S as 维保中心
    participant F as 财务中心 (CostLedger)
    participant Sale as 销售中心

    P->>F: PurchaseCompletedEvent<br/>recordCost(ACQUISITION, 采购价, PO单号)
    Note over F: totalCost = 采购价

    R->>F: RecoveryPaidEvent<br/>recordCost(ACQUISITION, 回收价, RO单号)
    Note over F: totalCost = 回收价

    S->>F: RepairCompletedEvent<br/>recordCost(REPAIR_PARTS, 配件费, 工单号)<br/>recordCost(REPAIR_LABOR, 人工费, 工单号)
    Note over F: totalCost += 配件费 + 人工费

    Sale->>F: StockOutEvent<br/>settleOut() → 返回 totalCost
    Note over F: 成本结转完成<br/>毛利 = 售价 - totalCost
```

## 结算单状态

```mermaid
stateDiagram-v2
    [*] --> DRAFT: 自动生成/手动创建
    DRAFT --> CONFIRMED: confirm() 确认
    CONFIRMED --> PAID: markPaid() 已付款
    PAID --> [*]
```

## 领域事件

### 消费的事件

| 事件 | 来源 | 处理逻辑 |
| :--- | :--- | :--- |
| `PurchaseCompletedEvent` | 采购中心 | 为每个 SN 创建 CostLedger，记录采购成本 |
| `RecoveryPaidEvent` | 回收中心 | 为每个 SN 创建 CostLedger，记录回收成本 |
| `RepairCompletedEvent` | 维保中心 | 在对应 SN 的 CostLedger 上追加维修成本 |
| `StockOutEvent` | 库存中心 | 执行成本结转，返回累计成本 |
| `SaleCompletedEvent` | 销售中心 | 生成应收结算项 |

### 发布的事件

当前版本财务中心不主动发布事件。

## 不变量

1. **成本单调递增**：CostLedger.totalCost 只能增加不能减少（特殊冲红场景除外）
2. **账户余额校验**：debit 操作前校验余额充足
3. **结算不可逆**：CONFIRMED 后不可回退到 DRAFT
