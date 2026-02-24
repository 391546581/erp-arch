# 成本归集全链路 (Cost Tracking)

> 以 SN 为维度的个别计价法，追踪每一分钱的来源。

## 成本归集流程

```mermaid
flowchart TD
    subgraph 成本来源["💰 成本来源"]
        C1["🛒 采购成本<br/>采购单价"]
        C2["♻️ 回收成本<br/>回收付款价"]
        C3["🔧 配件成本<br/>维修消耗配件"]
        C4["👷 人工成本<br/>维修工时"]
        C5["🚛 物流成本<br/>运输费用"]
    end

    subgraph 成本归集["📊 成本账本 (CostLedger)"]
        L["SN: iPhone15Pro-001<br/>━━━━━━━━━━━━━━<br/>ACQUISITION: ¥3,000<br/>REPAIR_PARTS: ¥150<br/>REPAIR_LABOR: ¥80<br/>LOGISTICS: ¥20<br/>━━━━━━━━━━━━━━<br/>totalCost: ¥3,250"]
    end

    subgraph 成本结转["📈 成本结转"]
        S["售价: ¥4,500"]
        M["毛利 = 售价 - totalCost<br/>= ¥4,500 - ¥3,250<br/>= ¥1,250"]
    end

    C1 -->|"PurchaseCompletedEvent"| L
    C2 -->|"RecoveryPaidEvent"| L
    C3 -->|"RepairCompletedEvent"| L
    C4 -->|"RepairCompletedEvent"| L
    C5 -->|"手动录入"| L
    
    L -->|"StockOutEvent"| S
    S --> M

    style L fill:#fff3e0
    style M fill:#e8f5e9
```

## 成本归集时序图

```mermaid
sequenceDiagram
    participant 采购 as 采购中心
    participant 回收 as 回收中心
    participant 维保 as 维保中心
    participant CL as CostLedger
    participant 库存 as 库存中心
    participant 结算 as 结算计算

    Note over CL: SN: iPhone15Pro-001

    alt 采购入库
        采购->>CL: recordCost(ACQUISITION, ¥3000, PO-001)
        Note over CL: totalCost = ¥3,000
    else 回收入库
        回收->>CL: recordCost(ACQUISITION, ¥2500, RO-001)
        Note over CL: totalCost = ¥2,500
    end

    维保->>CL: recordCost(REPAIR_PARTS, ¥150, WO-001)
    Note over CL: totalCost = ¥3,150
    
    维保->>CL: recordCost(REPAIR_LABOR, ¥80, WO-001)
    Note over CL: totalCost = ¥3,230

    opt 物流
        CL->>CL: recordCost(LOGISTICS, ¥20, -)
        Note over CL: totalCost = ¥3,250
    end

    库存->>CL: settleOut()
    CL-->>结算: return totalCost = ¥3,250
    
    Note over 结算: 售价 ¥4,500<br/>成本 ¥3,250<br/>毛利 ¥1,250
```

## 成本类型说明

| 成本类型 | 代码 | 来源事件 | 说明 |
| :--- | :--- | :--- | :--- |
| **获取成本** | `ACQUISITION` | PurchaseCompletedEvent / RecoveryPaidEvent | 每个 SN 只有一条，采购价或回收价 |
| **维修配件** | `REPAIR_PARTS` | RepairCompletedEvent | 可多次累加（多次维修） |
| **维修人工** | `REPAIR_LABOR` | RepairCompletedEvent | 可多次累加 |
| **物流运输** | `LOGISTICS` | 手动录入 | 运输/快递费用 |

## 关键业务规则

1. **单调递增**：totalCost 只增不减（不允许删除成本条目）
2. **唯一来源**：每个 SN 只能有一条 ACQUISITION 类型记录
3. **可追溯**：每条 CostEntry 必须关联来源单号 (sourceOrderId)
4. **结转时机**：出库确认 (StockOutEvent) 时执行成本结转
5. **毛利计算**：`毛利 = 实际售价 - settleOut() 返回的 totalCost`
