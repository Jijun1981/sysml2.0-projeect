# 需求域 API 契约草案（P1）

更新编号：REQ-API-P1-1

目标：为“仅需求阶段（P1）”提供最小可用的服务契约。实现不在本文范围，优先与现有架构（GraphQL）对齐，语义尽量复用官方模型概念。

## 1. GraphQL Schema（草案）
```graphql
# Types
scalar UUID

type Requirement {
  id: UUID!
  name: String!
  text: String!
  acceptanceCriteria: String
  priority: Int
  status: RequirementStatus
  derivedFrom: Requirement
  children: [Requirement!]!
}

enum RequirementStatus { PROPOSED APPROVED VERIFIED REJECTED }

# Queries
extend type Query {
  requirement(id: UUID!): Requirement
  requirements(
    filter: RequirementFilter
    after: UUID
    before: UUID
    pageSize: Int
  ): [Requirement!]!
}

input RequirementFilter {
  nameContains: String
  statusIn: [RequirementStatus!]
}

# Mutations
extend type Mutation {
  createRequirement(input: RequirementInput!): Requirement!
  updateRequirement(id: UUID!, input: RequirementUpdateInput!): Requirement!
  deleteRequirement(id: UUID!): Boolean!
  setRequirementParent(id: UUID!, parentId: UUID): Requirement!
}

input RequirementInput {
  name: String!
  text: String!
  acceptanceCriteria: String
  priority: Int
}

input RequirementUpdateInput {
  name: String
  text: String
  acceptanceCriteria: String
  priority: Int
  status: RequirementStatus
}

# Subscriptions（可选，P1 可暂缓）
# extend type Subscription {
#   requirementChanged(projectId: UUID!): RequirementEvent!
# }

# type RequirementEvent { id: UUID!, changeType: ChangeType!, payload: Requirement }
# enum ChangeType { ADDED MODIFIED DELETED }
```

## 2. 约束与错误语义
- 输入校验：`name`/`text` 必填；`pageSize` 合理上限（如 200）。
- 层级规则：`setRequirementParent` 禁止环依赖；禁止跨项目引用（由后端上下文控制）。
- 删除规则：若存在子节点需先迁移或级联删除（P1 默认禁止级联，返回错误）。
- 错误码建议：
  - REQ-VALIDATION-001：字段缺失/格式错误
  - REQ-HIERARCHY-001：层级循环
  - REQ-DOMAIN-001：跨项目引用
  - REQ-STATE-001：状态切换非法

## 3. 与后续阶段的衔接点
- P2：将新增需求到部件的 Allocation/Satisfies 相关接口；
- P3：引入校验触发与验证结果回写，支持将需求推进到 VERIFIED；
- P4：订阅与报表（覆盖率/验证报告）。
