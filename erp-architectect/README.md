# fprice-erp-saas 架构设计中心

> **Architecture-as-Code** — 所有设计图表以 Mermaid 代码存储在 Git 仓库中，像代码一样迭代演进。

## 设计原则

📖 [DESIGN_PRINCIPLES.md](./DESIGN_PRINCIPLES.md) — 务实型 DDD 设计原则

## 战略设计 (Strategic Design)

从全局视角理解系统的限界上下文划分与关系。

| 文件 | 内容 |
| :--- | :--- |
| [context-map.md](./strategic/context-map.md) | 限界上下文映射图，展示所有上下文的关系与边界 |
| [domain-classification.md](./strategic/domain-classification.md) | 核心域 / 支撑域 / 通用域分类及理由 |
| [integration-patterns.md](./strategic/integration-patterns.md) | 上下文间集成模式（事件/依赖/共享内核） |

## 战术设计 (Tactical Design)

深入到每个限界上下文内部的聚合、实体、状态机与事件设计。

| 上下文 | 分类 | 入口 |
| :--- | :--- | :--- |
| 📦 库存中心 (Inventory) | 核心域 | [tactical/inventory/](./tactical/inventory/) |
| 🔍 质检中心 (Quality) | 核心域 | [tactical/quality/](./tactical/quality/) |
| 🛒 采购中心 (Purchase) | 核心支撑域 | [tactical/purchase/](./tactical/purchase/) |
| 💰 销售中心 (Sale) | 核心支撑域 | [tactical/sale/](./tactical/sale/) |
| ♻️ 回收中心 (Recovery) | 核心支撑域 | [tactical/recovery/](./tactical/recovery/) |
| 🔧 维保中心 (Service) | 核心支撑域 | [tactical/service/](./tactical/service/) |
| 💳 财务中心 (Finance) | 价值域 | [tactical/finance/](./tactical/finance/) |
| 📱 商品中心 (Product) | 通用域 | [tactical/product/](./tactical/product/) |
| 🏢 渠道中心 (Channel) | 支撑域 | [tactical/channel/](./tactical/channel/) |
| 👥 客户中心 (CRM) | 支撑域 | [tactical/crm/](./tactical/crm/) |
| 🏛️ 组织中心 (Org) | 支撑域 | [tactical/org/](./tactical/org/) |

## 业务流程 (Business Process)

跨上下文的端到端业务流程图。

| 文件 | 内容 |
| :--- | :--- |
| [sn-lifecycle.md](./process/sn-lifecycle.md) | SN 全生命周期（采购→质检→销售→售后） |
| [cost-tracking.md](./process/cost-tracking.md) | 成本归集全链路（个别计价法） |

## 迭代工具

| 文件 | 用途 |
| :--- | :--- |
| [CHANGELOG.md](./CHANGELOG.md) | 架构变更日志，记录每次设计迭代 |
| [REVIEW_CHECKLIST.md](./REVIEW_CHECKLIST.md) | 架构自审清单 |

## 如何预览 Mermaid 图表

**VS Code**：安装 [Markdown Preview Mermaid Support](https://marketplace.visualstudio.com/items?itemName=bierner.markdown-mermaid) 插件，按 `Ctrl+Shift+V` 预览。

**其他方式**：
- [Mermaid Live Editor](https://mermaid.live/) — 在线编辑器
- GitHub / GitLab 原生支持 Mermaid 渲染
- 也可导出为 PlantUML 或 draw.io 格式进行编辑
