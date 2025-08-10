# SysML v2 建模平台 - 技术开发Roadmap v3（补丁版）

> 基于v2对齐版，应用6个关键补丁，确保契约一致性和可执行性

## 📍 核心原则

1. **契约先行**: GraphQL schema定义在实现之前，统一Payload模式
2. **最小可用**: 每个Sprint产出可工作软件
3. **MECE执行**: 严格职责边界，无重叠
4. **DAG依赖**: 无循环依赖，清晰层次
5. **渐进式架构**: 从简单到复杂，持续演进

## ⚠️ 关键约定（补丁强化）

### GraphQL统一返回模式
```graphql
# 所有Mutation统一使用Payload模式
type MutationPayload {
  ok: Boolean!
  error: Error
  # item字段在具体Payload中定义
}

type Error {
  code: String!
  messageKey: String!
  path: [String!]!
  details: JSON
}
```

### 生产环境安全
```yaml
GraphQL配置:
  dev:
    introspection: true
    playground: true
  prod:
    introspection: false  # 或仅管理员权限
    playground: false
```

## 📊 Wave 0: Foundation (2周)

### Sprint 1: 基础设施搭建

#### EP-API 核心契约（补丁1：统一Payload）
```graphql
# core.graphqls - 统一基础契约
scalar DateTime
scalar JSON

type Error {
  code: String!
  messageKey: String!
  path: [String!]!
  details: JSON
}

type PageInfo {
  total: Int!
  page: Int!
  size: Int!
  hasNext: Boolean!
}

# 探活查询
type Query {
  ok: Boolean!
}

# 健康检查
type HealthStatus {
  status: String! # UP/DOWN
  components: JSON!
}
```

#### 安全配置（补丁6：prod安全）
```java
@Configuration
public class GraphQLSecurityConfig {
    @Value("${spring.profiles.active}")
    private String profile;
    
    @Bean
    public GraphQL graphQL(GraphQLSchema schema) {
        GraphQL.Builder builder = GraphQL.newGraphQL(schema);
        
        if ("prod".equals(profile)) {
            // 生产环境关闭introspection
            builder.fieldVisibility(NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY);
        }
        
        return builder.build();
    }
}
```

## 📊 Wave 1: Requirements Domain - P1 (2周)

### Sprint 3: 需求核心功能

#### 需求GraphQL契约（补丁1+2：Payload模式+移除@unique）
```graphql
# requirements.graphqls
type RequirementDefinition {
  id: ID!
  reqId: String! # 唯一约束通过服务端校验，不使用@unique指令
  name: String!
  text: String
  parent: RequirementDefinition
  children: [RequirementDefinition!]!
  createdAt: DateTime!
  updatedAt: DateTime!
}

input CreateRequirementInput {
  reqId: String!
  name: String!
  text: String
  parentId: ID
}

# 统一Payload返回模式
type CreateRequirementPayload {
  ok: Boolean!
  error: Error
  requirement: RequirementDefinition
}

type UpdateRequirementPayload {
  ok: Boolean!
  error: Error
  requirement: RequirementDefinition
}

type DeleteRequirementPayload {
  ok: Boolean!
  error: Error
  deletedId: ID
}

type Mutation {
  createRequirement(input: CreateRequirementInput!): CreateRequirementPayload!
  updateRequirement(id: ID!, input: UpdateRequirementInput!): UpdateRequirementPayload!
  deleteRequirement(id: ID!): DeleteRequirementPayload!
}
```

#### 服务端唯一性校验（补丁2：替代@unique）
```java
@Service
public class RequirementUseCase {
    @Transactional
    public CreateRequirementPayload createRequirement(CreateRequirementInput input) {
        // 唯一性校验（替代@unique指令）
        if (repository.existsByReqId(input.getReqId())) {
            return CreateRequirementPayload.builder()
                .ok(false)
                .error(Error.builder()
                    .code("REQ_ID_DUPLICATE")
                    .messageKey("error.requirement.duplicate")
                    .path(Arrays.asList("input", "reqId"))
                    .build())
                .build();
        }
        
        // 创建需求
        RequirementDefinition req = createRequirementDefinition(input);
        
        return CreateRequirementPayload.builder()
            .ok(true)
            .requirement(req)
            .build();
    }
}
```

## 📊 Wave 2: Structure & Properties - P2 (2周)

### 结构域契约（补丁1：Payload模式）
```graphql
# structure.graphqls
type CreatePartPayload {
  ok: Boolean!
  error: Error
  part: PartUsage
}

type CreateConnectionPayload {
  ok: Boolean!
  error: Error
  connection: Connection
}

type Mutation {
  createPart(input: CreatePartInput!): CreatePartPayload!
  createConnection(sourcePortId: ID!, targetPortId: ID!): CreateConnectionPayload!
}
```

## 📊 Wave 3: Constraints - P3 (2周)

### 执行策略（补丁4：同步优先）
```yaml
P3执行策略演进:
  初始版本(P3):
    模式: 同步执行
    超时: 5秒保护
    实现: 
      - 直接计算
      - 同步返回结果
      - 超时则返回错误
    
  优化版本(P4/P5):
    模式: 异步队列
    实现:
      - 任务队列
      - 线程池
      - 结果轮询/订阅
```

```java
// P3阶段：同步执行（简单可靠）
@Service
public class ConstraintExecutor {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    
    public ConstraintResult executeSync(String constraintId, Map<String, Object> inputs) {
        try {
            // 同步执行，带超时保护
            return CompletableFuture
                .supplyAsync(() -> evaluateFormula(constraintId, inputs))
                .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return ConstraintResult.timeout(constraintId);
        }
    }
    
    // P4/P5再引入异步队列
    // @Async
    // public CompletableFuture<ConstraintResult> executeAsync(...) { ... }
}
```

## 📊 Wave 4: Traceability - P4 (2周)

### 追溯域契约（补丁3：Element union定义）
```graphql
# traceability.graphqls

# Element联合类型定义
union Element = RequirementDefinition | RequirementUsage | PartUsage | PartDefinition | FunctionUsage

# Evidence类型定义
type Evidence {
  id: ID!
  type: EvidenceType! # CALCULATION | TEST | REVIEW | SIMULATION
  sourceId: ID!      # 约束结果ID或测试ID
  status: EvidenceStatus! # PASS | FAIL | PENDING
  timestamp: DateTime!
  details: JSON      # 具体证据内容
}

enum EvidenceType {
  CALCULATION  # 来自约束计算
  TEST        # 来自测试执行
  REVIEW      # 来自人工评审
  SIMULATION  # 来自仿真结果
}

enum EvidenceStatus {
  PASS
  FAIL
  PENDING
}

type TraceRelation {
  id: ID!
  type: TraceType! # SATISFY | VERIFY | ALLOCATE
  source: Element!
  target: Element!
  evidence: [Evidence!]! # 只读引用，不触发计算
  createdAt: DateTime!
}

# Payload模式
type CreateTraceRelationPayload {
  ok: Boolean!
  error: Error
  relation: TraceRelation
}

type Mutation {
  createTraceRelation(
    type: TraceType!
    sourceId: ID!
    targetId: ID!
    evidenceIds: [ID!]
  ): CreateTraceRelationPayload!
}
```

## 📊 Wave 5: Output Layer - P5 (2周)

### 导入导出契约（补丁1：Payload模式）
```graphql
# output.graphqls
type ExportPayload {
  ok: Boolean!
  error: Error
  downloadUrl: String
  format: ExportFormat!
}

type ImportPayload {
  ok: Boolean!
  error: Error
  importedCount: Int
  errors: [ImportError!]
}

type ImportError {
  line: Int
  message: String!
  elementId: String
}

type Mutation {
  exportModel(format: ExportFormat!, scope: ExportScope): ExportPayload!
  importModel(file: Upload!, format: ExportFormat!): ImportPayload!
}
```

## 📈 基准数据集定义（补丁5）

| 数据集 | 元素数 | 关系数 | 用途 | 查询P50目标 | 变更P50目标 | 内存预算 |
|-------|--------|--------|------|------------|------------|---------|
| **Small** | 100 | 150 | 开发测试、单元测试 | <100ms | <200ms | <100MB |
| **Medium** | 1,000 | 2,000 | 用户验收、集成测试 | <200ms | <400ms | <500MB |
| **Large** | 10,000 | 20,000 | 生产负载、性能测试 | <500ms | <800ms | <2GB |

### 性能验证脚本
```javascript
// 性能基准测试
const performanceBaseline = {
  small: {
    elements: 100,
    relationships: 150,
    targets: {
      query_p50: 100,
      mutation_p50: 200,
      memory_max: 100 * 1024 * 1024 // 100MB
    }
  },
  medium: {
    elements: 1000,
    relationships: 2000,
    targets: {
      query_p50: 200,
      mutation_p50: 400,
      memory_max: 500 * 1024 * 1024 // 500MB
    }
  },
  large: {
    elements: 10000,
    relationships: 20000,
    targets: {
      query_p50: 500,
      mutation_p50: 800,
      memory_max: 2 * 1024 * 1024 * 1024 // 2GB
    }
  }
};

async function verifyPerformance(dataset) {
  const config = performanceBaseline[dataset];
  await loadTestData(config.elements, config.relationships);
  
  const metrics = await runPerformanceTest();
  
  assert(metrics.query_p50 <= config.targets.query_p50,
    `Query P50 ${metrics.query_p50}ms exceeds target ${config.targets.query_p50}ms`);
  assert(metrics.mutation_p50 <= config.targets.mutation_p50,
    `Mutation P50 ${metrics.mutation_p50}ms exceeds target ${config.targets.mutation_p50}ms`);
  assert(metrics.memory_used <= config.targets.memory_max,
    `Memory ${metrics.memory_used} exceeds budget ${config.targets.memory_max}`);
}
```

## 🔍 完整GraphQL Schema示例文件

<details>
<summary>点击展开完整的schema示例</summary>

```graphql
# schema/complete-example.graphqls
# 这是一个完整的契约示例，展示所有Payload模式

# ========== 基础类型 ==========
scalar DateTime
scalar JSON
scalar Upload

# ========== 错误模型 ==========
type Error {
  code: String!
  messageKey: String!
  path: [String!]!
  details: JSON
}

# ========== 分页 ==========
type PageInfo {
  total: Int!
  page: Int!
  size: Int!
  hasNext: Boolean!
}

# ========== Element联合 ==========
union Element = RequirementDefinition | RequirementUsage | PartUsage | PartDefinition

# ========== 需求域 ==========
type RequirementDefinition {
  id: ID!
  reqId: String! # 服务端唯一性校验
  name: String!
  text: String
  parent: RequirementDefinition
  children: [RequirementDefinition!]!
  createdAt: DateTime!
  updatedAt: DateTime!
}

type RequirementUsage {
  id: ID!
  definition: RequirementDefinition!
  parent: RequirementUsage
  children: [RequirementUsage!]!
}

# ========== Payload模式示例 ==========
type CreateRequirementPayload {
  ok: Boolean!
  error: Error
  requirement: RequirementDefinition
}

type UpdateRequirementPayload {
  ok: Boolean!
  error: Error
  requirement: RequirementDefinition
}

type DeletePayload {
  ok: Boolean!
  error: Error
  deletedId: ID
}

type QueryRequirementsPayload {
  ok: Boolean!
  error: Error
  items: [RequirementDefinition!]!
  pageInfo: PageInfo!
}

# ========== Mutations ==========
type Mutation {
  # 需求域
  createRequirement(input: CreateRequirementInput!): CreateRequirementPayload!
  updateRequirement(id: ID!, input: UpdateRequirementInput!): UpdateRequirementPayload!
  deleteRequirement(id: ID!): DeletePayload!
  
  # 结构域
  createPart(input: CreatePartInput!): CreatePartPayload!
  createConnection(sourcePortId: ID!, targetPortId: ID!): CreateConnectionPayload!
  
  # 追溯域
  createTraceRelation(
    type: TraceType!
    sourceId: ID!
    targetId: ID!
    evidenceIds: [ID!]
  ): CreateTraceRelationPayload!
  
  # 输出层
  exportModel(format: ExportFormat!, scope: ExportScope): ExportPayload!
  importModel(file: Upload!, format: ExportFormat!): ImportPayload!
}

# ========== Queries ==========
type Query {
  # 健康检查
  ok: Boolean!
  health: HealthStatus!
  
  # 需求查询
  requirement(id: ID!): RequirementDefinition
  requirements(
    filter: RequirementFilter
    page: Int = 1
    size: Int = 20
    sort: RequirementSort
  ): QueryRequirementsPayload!
  
  # 追溯查询
  traceRelations(sourceId: ID, targetId: ID, type: TraceType): [TraceRelation!]!
  coverageMatrix(requirementIds: [ID!]!): CoverageMatrix!
}

# ========== Subscriptions ==========
type Subscription {
  modelChanged(scope: SubscriptionScope): ModelChangeEvent!
}

type ModelChangeEvent {
  id: ID!
  type: String!
  changeType: ChangeType! # CREATE | UPDATE | DELETE
  timestamp: DateTime!
  userId: String
}
```

</details>

## 🚀 立即行动清单（应用补丁后）

```bash
# 1. 创建统一的GraphQL契约文件
cat > server/api/graphql/schema/base-types.graphqls << 'EOF'
scalar DateTime
scalar JSON

type Error {
  code: String!
  messageKey: String!
  path: [String!]!
  details: JSON
}

# 所有Mutation返回Payload
interface MutationPayload {
  ok: Boolean!
  error: Error
}
EOF

# 2. 生成Payload模板生成器
cat > scripts/dev/generate-payload.sh << 'EOF'
#!/bin/bash
# 用法: ./generate-payload.sh CreateRequirement requirement:RequirementDefinition
OPERATION=$1
RETURN_TYPE=$2

cat << GRAPHQL
type ${OPERATION}Payload implements MutationPayload {
  ok: Boolean!
  error: Error
  ${RETURN_TYPE}
}
GRAPHQL
EOF

# 3. 配置环境相关的introspection
cat > server/src/main/resources/application.yml << 'EOF'
graphql:
  introspection:
    enabled: ${GRAPHQL_INTROSPECTION_ENABLED:true}
  playground:
    enabled: ${GRAPHQL_PLAYGROUND_ENABLED:true}
    
---
spring:
  profiles: prod
graphql:
  introspection:
    enabled: false
  playground:
    enabled: false
EOF

# 4. 创建性能基准测试
cat > test/performance/baseline-test.js << 'EOF'
const datasets = {
  small: { elements: 100, p50Target: 100 },
  medium: { elements: 1000, p50Target: 200 },
  large: { elements: 10000, p50Target: 500 }
};

async function runBaselineTest(datasetName) {
  const dataset = datasets[datasetName];
  console.log(`Testing ${datasetName}: ${dataset.elements} elements`);
  
  const result = await measureP50(dataset.elements);
  
  if (result > dataset.p50Target) {
    throw new Error(`P50 ${result}ms exceeds target ${dataset.p50Target}ms`);
  }
  
  console.log(`✅ ${datasetName} passed: P50=${result}ms`);
}
EOF
```

## ✅ 补丁应用总结

| 补丁 | 状态 | 影响范围 | 验证方法 |
|------|------|---------|---------|
| 1. 统一Payload返回 | ✅ | 所有Mutation | Schema编译通过 |
| 2. 移除@unique | ✅ | 需求域 | 服务端校验测试 |
| 3. Element union定义 | ✅ | 追溯域 | Union类型解析 |
| 4. 同步执行优先 | ✅ | 约束域P3 | 超时测试 |
| 5. 基准数据集表 | ✅ | 全局 | 性能测试脚本 |
| 6. Prod安全配置 | ✅ | Foundation | 环境配置验证 |

---

**本Roadmap v3已应用所有补丁，确保契约一致、边界清晰、可直接执行。**