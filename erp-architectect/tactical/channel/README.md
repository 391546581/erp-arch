# 🏢 渠道中心 (Channel Context)

> **分类**：📎 支撑域 | **建模级别**：L2 轻量领域
>
> 对接外部销售渠道（闲鱼、拼多多、抖音等），负责数据格式适配和订单同步。

## 职责边界

- ✅ 管理渠道上架（发布/下架商品到外部平台）
- ✅ 同步渠道订单到内部系统
- ✅ 渠道商品映射（内部 SKU ↔ 渠道商品 ID）
- ✅ 渠道结算数据对接
- ❌ 不处理内部库存逻辑
- ❌ 不处理内部订单流程

## 聚合设计

```mermaid
classDiagram
    class ChannelListing {
        <<聚合根>>
        -ListingId listingId
        -ChannelType channelType
        -SnCode snCode
        -String channelProductId
        -ListingStatus status
        -String publishTemplate
        +publish()
        +delist()
        +syncOrderStatus(channelOrderId)
    }

    class ChannelType {
        <<枚举>>
        IDLEFISH
        PDD
        DOUYIN
    }

    class ListingStatus {
        <<枚举>>
        DRAFT
        LISTED
        SOLD
        DELISTED
    }

    ChannelListing --> ChannelType
    ChannelListing --> ListingStatus
```

## 防腐层 (Anti-Corruption Layer)

```mermaid
flowchart LR
    subgraph 外部平台
        A1["闲鱼 API"]
        A2["拼多多 API"]
        A3["抖音 API"]
    end

    subgraph 渠道中心 ACL
        B1["IdleFishAdapter"]
        B2["PddAdapter"]
        B3["DouyinAdapter"]
        B4["ChannelService"]
    end

    subgraph 内部系统
        C1["销售中心"]
    end

    A1 --> B1
    A2 --> B2
    A3 --> B3
    B1 --> B4
    B2 --> B4
    B3 --> B4
    B4 -->|"ChannelOrderSyncEvent"| C1
```

## 领域事件

### 发布的事件

| 事件 | 触发条件 | 消费者 | 携带数据 |
| :--- | :--- | :--- | :--- |
| `ChannelOrderSyncEvent` | 渠道订单同步 | 销售中心 | channelType, channelOrderId, items, buyerInfo |

> 💡 **设计建议**：渠道中心的核心价值在于 ACL（防腐层），将外部平台的各种数据格式统一转换为内部领域模型。每新增一个渠道，只需添加一个新的 Adapter 实现。
