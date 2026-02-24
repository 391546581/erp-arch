# SN 全生命周期流程 (SN Lifecycle)

> SN (Serial Number) 是系统的核心追踪标识。本图展示一个 SN 从进入系统到离开系统的完整旅程。

## 全链路流程图

```mermaid
flowchart TD
    subgraph 供给源["📥 供给源"]
        P["🛒 采购收货<br/>采购中心"]
        R["♻️ 回收确认<br/>回收中心"]
    end

    subgraph 质控["🔍 质量控制"]
        QS["开始质检<br/>质检中心"]
        QR["质检评级<br/>S/A/B/C/D"]
    end

    subgraph 维修["🔧 维修"]
        RP["创建工单<br/>维保中心"]
        RPD["维修处理<br/>配件+人工"]
        RPC["维修完成<br/>成本累加"]
    end

    subgraph 上架销售["💰 上架与销售"]
        IS["库存可售<br/>IN_STOCK"]
        CL["渠道上架<br/>渠道中心"]
        SO["创建销售订单<br/>销售中心"]
        LK["锁定库存<br/>LOCKED"]
        OUT["出库确认<br/>SOLD"]
    end

    subgraph 终态["🏁 终态"]
        SOLD["✅ 已售出"]
        SCRAP["❌ 报废"]
    end

    subgraph 财务同步["💳 财务（全程伴随）"]
        F1["记录采购/回收成本"]
        F2["累加维修成本"]
        F3["成本结转 + 毛利计算"]
    end

    %% 主流程
    P -->|"PurchaseCompletedEvent"| PI["📦 入库<br/>PENDING_INSPECT"]
    R -->|"RecoveryPaidEvent"| PI
    
    PI --> QS
    QS --> QR
    
    QR -->|"S/A 级 良品"| IS
    QR -->|"B/C 级 需维修"| RP
    QR -->|"D 级 报废"| SCRAP
    
    RP --> RPD
    RPD --> RPC
    RPC -->|"复检"| QS
    
    IS --> CL
    IS --> SO
    CL -->|"ChannelOrderSyncEvent"| SO
    SO --> LK
    LK --> OUT
    OUT --> SOLD

    %% 财务同步线
    PI -.->|"初始成本"| F1
    RPC -.->|"维修成本"| F2
    OUT -.->|"StockOutEvent"| F3

    %% 异常分支
    LK -->|"订单取消"| IS

    style PI fill:#e1f5fe
    style IS fill:#e8f5e9
    style SOLD fill:#c8e6c9
    style SCRAP fill:#ffcdd2
    style LK fill:#fff9c4
```

## SN 状态与所属上下文对照

| SN 状态 | 所属上下文 | 触发动作 | 下一步 |
| :--- | :--- | :--- | :--- |
| `PENDING_INSPECT` | 库存中心 | 采购/回收入库 | 分配质检 |
| `INSPECTING` | 库存中心 (质检中心操作) | 开始质检 | 评级结果 |
| `IN_STOCK` | 库存中心 | 质检通过 | 上架/锁定 |
| `NEED_REPAIR` | 库存中心 | 质检不通过 | 创建工单 |
| `REPAIRING` | 库存中心 (维保中心操作) | 维修中 | 维修完成 |
| `REPAIRED` | 库存中心 | 维修完成 | 复检 |
| `LOCKED` | 库存中心 | 订单锁定 | 出库/释放 |
| `SOLD` | 库存中心 | 出库确认 | 终态 |
| `SCRAPPED` | 库存中心 | 报废 | 终态 |

## 关键业务规则

1. **入口唯二**：SN 只能通过采购或回收两种方式进入系统
2. **质检必经**：所有新入库的 SN 必须先经过质检
3. **维修可循环**：维修 → 复检 → 维修 可多次循环
4. **锁定排他**：一个 SN 同时只能被一个订单锁定
5. **终态不可逆**：SOLD 和 SCRAPPED 是终态，不可回退
