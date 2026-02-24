---
description: DDD 架构迭代流程 — 如何在 AI 辅助下演进架构设计
---

# DDD 架构迭代流程

当你想要调整架构设计时，按以下步骤与 AI 协作：

## 场景 A：调整某个上下文的聚合设计

1. 描述你想调整的业务场景（例如："质检中增加复检环节"）
2. AI 读取 `d:\erp-architectect\tactical\{context}\` 下的相关文件
3. AI 生成修改建议（以 Mermaid diff 形式展示变更）
4. 你确认后 AI 应用变更到对应文件
5. AI 更新 `CHANGELOG.md` 记录变更

## 场景 B：新增业务流程

1. 描述新业务流程（例如："增加调拨流程"）
2. AI 分析涉及的限界上下文，更新 `strategic/context-map.md`
3. AI 为涉及的上下文创建/更新战术设计文件
4. AI 在 `process/` 下创建新的流程图文件
5. 更新 `README.md` 导航索引

## 场景 C：从代码反向更新架构图

1. 指定代码所在的模块（例如："根据 erp2-finance 的代码更新架构图"）
// turbo
2. AI 扫描 `d:\erp-model\{module}\` 中的 Java 代码
3. AI 对比代码与 `d:\erp-architectect\tactical\{context}\` 中的设计文件
4. AI 标记差异并生成更新建议
5. 你确认后 AI 应用变更

## 场景 D：架构决策记录 (ADR)

1. 描述决策问题（例如："质检结果应该存在质检中心还是库存中心？"）
2. AI 列出选项及其利弊
3. 你做出决策
4. AI 记录到 `CHANGELOG.md` 中
