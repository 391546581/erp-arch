# 💰 销售中心 (Sale Context)

> **分类**：🔗 核心支撑域 | **建模级别**：L2 轻量领域
>
> 多渠道销售订单管理，涵盖前台销售、批量销售、渠道同步。

## 职责边界

- ✅ 管理销售订单的创建、支付、发货、收货
- ✅ 管理售后单（退货退款/换货/维修）
- ✅ 与库存中心协调锁库/出库
- ❌ 不直接操作库存状态
- ❌ 不直接处理渠道对接逻辑（由渠道中心适配）

## 聚合设计

```mermaid
classDiagram
    class SalesOrder {
        <<聚合根>>
        -SalesOrderId orderId
        -CustomerId customerId
        -ChannelType channelType
        -SalesStatus status
        -List~SalesItem~ items
        -Money totalAmount
        -PaymentInfo payment
        -LogisticsInfo logistics
        +create(items)
        +pay(paymentInfo)
        +ship(logisticsInfo)
        +complete()
        +cancel()
        +requestRefund(reason)
    }

    class SalesItem {
        <<实体>>
        -SkuId skuId
        -SnCode snCode
        -Money salePrice
        -Money cost
    }

    class AfterSaleOrder {
        <<聚合根>>
        -AfterSaleId afterSaleId
        -SalesOrderId salesOrderId
        -AfterSaleType type
        -List~AfterSaleItem~ items
        -AfterSaleStatus status
        +approve()
        +complete()
    }

    class SalesStatus {
        <<枚举>>
        CREATED
        PAID
        SHIPPING
        DELIVERED
        COMPLETED
        CANCELLED
        REFUNDING
        AFTER_SALE
    }

    class ChannelType {
        <<枚举>>
        POS
        APP
        IDLEFISH
        PDD
        DOUYIN
        OTHER
    }

    class AfterSaleType {
        <<枚举>>
        RETURN_REFUND
        EXCHANGE
        REPAIR
    }

    SalesOrder "1" *-- "*" SalesItem
    SalesOrder --> SalesStatus
    SalesOrder --> ChannelType
    AfterSaleOrder --> AfterSaleType
```

## 状态机

```mermaid
stateDiagram-v2
    [*] --> CREATED: create() 创建订单+锁库
    CREATED --> PAID: pay() 支付确认
    PAID --> SHIPPING: ship() 出库发货
    SHIPPING --> DELIVERED: 签收
    DELIVERED --> COMPLETED: complete() 确认收货

    CREATED --> CANCELLED: cancel() + 释放库存
    PAID --> REFUNDING: requestRefund()
    DELIVERED --> AFTER_SALE: 售后申请
    
    COMPLETED --> [*]
    CANCELLED --> [*]

    note right of CREATED
        锁定 SN 库存
        调用库存中心 lockForOrder
    end note

    note right of COMPLETED
        触发 SaleCompletedEvent
        → 成本结转
    end note
```

## 领域事件

### 发布的事件

| 事件 | 触发条件 | 消费者 | 携带数据 |
| :--- | :--- | :--- | :--- |
| `SaleCompletedEvent` | complete() | 财务中心 | orderId, items[snCode, salePrice] |

### 消费的事件

| 事件 | 来源 | 处理逻辑 |
| :--- | :--- | :--- |
| `ChannelOrderSyncEvent` | 渠道中心 | 自动创建内部销售订单 |
