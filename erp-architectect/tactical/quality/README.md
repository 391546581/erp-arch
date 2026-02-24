# 🔍 质检中心 (Quality Context)

> **分类**：⭐ 核心域 | **建模级别**：L3 充血模型
> 
> 质检评级直接决定商品流向，质检标准是区分商品品质的核心能力。

## 职责边界

- ✅ 管理质检模板（检测项定义）
- ✅ 执行质检流程，生成质检报告
- ✅ 根据模板规则自动计算评级
- ✅ 决定 SN 的流向（上架/维修/报废）
- ❌ 不直接修改库存状态（通过事件通知库存中心）

## 聚合设计

```mermaid
classDiagram
    class QualityTemplate {
        <<聚合根>>
        -TemplateId templateId
        -String name
        -CategoryId categoryId
        -List~CheckGroup~ checkGroups
        +addCheckGroup(groupName, items)
        +removeCheckGroup(groupId)
        +validate()
    }

    class CheckGroup {
        <<实体>>
        -String groupId
        -String name
        -String dimension
        -List~CheckItem~ items
    }

    class CheckItem {
        <<值对象>>
        -String itemName
        -String standard
        -Integer weight
        -List~String~ options
    }

    class InspectionReport {
        <<聚合根>>
        -ReportId reportId
        -SnCode snCode
        -TemplateId templateId
        -String inspector
        -Map~String_CheckResult~ results
        -QualityGrade grade
        -FlowDecision decision
        -LocalDateTime inspectedAt
        +evaluate(results)
        +decide(grade)
    }

    class QualityGrade {
        <<枚举>>
        S
        A
        B
        C
        D
    }

    class FlowDecision {
        <<枚举>>
        DIRECT_SALE
        NEED_REPAIR
        SCRAP
    }

    QualityTemplate "1" *-- "*" CheckGroup
    CheckGroup "1" *-- "*" CheckItem
    InspectionReport --> QualityGrade
    InspectionReport --> FlowDecision
```

## 评级→流向映射规则

```mermaid
flowchart LR
    S["S 级 (全新/完美)"] --> SALE["DIRECT_SALE 直接上架"]
    A["A 级 (轻微使用痕迹)"] --> SALE
    B["B 级 (明显使用痕迹)"] --> REPAIR["NEED_REPAIR 需维修"]
    C["C 级 (功能缺陷)"] --> REPAIR
    D["D 级 (严重损坏)"] --> SCRAP["SCRAP 报废"]
```

## 领域事件

### 发布的事件

| 事件 | 触发条件 | 消费者 | 携带数据 |
| :--- | :--- | :--- | :--- |
| `InspectionCompletedEvent` | evaluate() + decide() | 库存中心 | snCode, grade, decision, reportId |

### 消费的事件

无。质检中心不消费其他上下文的事件，而是由库存中心在 SN 进入 INSPECTING 状态时，通过应用服务调用质检中心。

## 不变量

1. **质检完整性**：质检报告必须包含模板中所有检测项的结果
2. **评级不可逆**：一份质检报告一旦评级完成，不可修改（如需重评，创建新报告）
3. **流向确定性**：评级到流向的映射必须是确定性的（S/A→上架，B/C→维修，D→报废）
