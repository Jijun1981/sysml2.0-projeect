# SysML v2 API Services 项目总结

## 项目概述

SysML v2 API Services 是 OMG SysML v2 标准的官方参考实现，提供了完整的 REST/HTTP API 服务，用于管理和操作 SysML v2 模型。

## 核心功能实现

### 1. 版本化模型管理
- **Project** - 项目管理，作为模型的顶层容器
- **Commit** - 提交机制，实现模型版本控制
- **Branch** - 分支管理，支持并行开发
- **Tag** - 标签管理，标记重要版本
- **DataVersion** - 数据版本追踪

### 2. SysML v2 元模型完整支持
项目实现了 **150+ 个元模型类**，包括：
- **结构建模**：PartDefinition、PartUsage、ConnectionDefinition、ConnectionUsage、InterfaceDefinition 等
- **行为建模**：ActionDefinition、ActionUsage、StateDefinition、StateUsage、TransitionUsage 等
- **需求建模**：RequirementDefinition、RequirementUsage、ConstraintDefinition、ConstraintUsage 等
- **分析建模**：AnalysisCaseDefinition、VerificationCaseDefinition、ViewDefinition 等
- **表达式**：LiteralExpression、OperatorExpression、InvocationExpression 等

### 3. REST API 接口设计
```
基础路径: http://localhost:9000/

主要端点：
- /projects                     - 项目管理
- /projects/{id}/commits        - 提交管理
- /projects/{id}/branches       - 分支管理
- /projects/{id}/tags          - 标签管理
- /commits/{id}/elements        - 元素操作
- /elements/{id}               - 单个元素CRUD
- /elements/{id}/relationships  - 关系管理
- /query                       - 查询接口
```

### 4. 查询能力
- 支持复杂的查询条件组合
- 支持分页和排序
- 支持元素类型过滤
- 支持关系遍历查询

## 技术架构

### 1. 技术栈
```
框架层：
- Play Framework 2.8    - Web框架
- Java 11              - 开发语言
- Scala Build Tool     - 构建工具

持久层：
- JPA/Hibernate        - ORM框架
- PostgreSQL           - 数据库
- UUID主键策略         - 全局唯一标识

API层：
- RESTful API          - 接口风格
- JSON/JSON-LD         - 数据格式
- Swagger/OpenAPI      - API文档
```

### 2. 分层架构

```
┌─────────────────────────────────────┐
│   Controllers (REST端点)             │
│   - ProjectController               │
│   - CommitController                │
│   - ElementController               │
│   - RelationshipController          │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Services (业务逻辑)                │
│   - ProjectService                  │
│   - CommitService                   │
│   - ElementService                  │
│   - QueryService                    │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   DAO Layer (数据访问)               │
│   - JpaProjectDao                   │
│   - JpaCommitDao                    │
│   - JpaElementDao                   │
│   - JpaRelationshipDao              │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   JPA/Hibernate                     │
│   - 实体映射                         │
│   - 事务管理                         │
│   - 查询构建                         │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   PostgreSQL Database               │
│   - 模型数据持久化                   │
│   - 版本历史存储                     │
│   - 索引优化                         │
└─────────────────────────────────────┘
```

### 3. 核心设计模式

#### 3.1 版本控制模式
```java
// 每次修改创建新的提交
Commit commit = new CommitImpl();
commit.setPreviousCommit(parentCommit);
commit.setDataVersion(newDataVersion);
commit.setCommitDate(ZonedDateTime.now());
```

#### 3.2 元素继承体系
```java
Element (根接口)
├── Namespace
│   ├── Package
│   ├── Type
│   │   ├── Classifier
│   │   │   ├── Class
│   │   │   └── DataType
│   │   └── Feature
│   │       ├── Usage
│   │       │   ├── PartUsage
│   │       │   ├── RequirementUsage
│   │       │   └── ConstraintUsage
│   │       └── Definition
│   │           ├── PartDefinition
│   │           ├── RequirementDefinition
│   │           └── ConstraintDefinition
└── Relationship
    ├── Specialization
    ├── FeatureTyping
    └── Dependency
```

#### 3.3 工作副本机制
```java
// WorkingDataVersion 用于跟踪未提交的修改
WorkingDataVersion working = new WorkingDataVersionImpl();
working.setBaseCommit(currentCommit);
working.setPayload(modifiedElements);
```

### 4. 数据持久化策略

#### 4.1 实体映射
- 使用 **单表继承策略** (Single Table Inheritance)
- 所有元模型类映射到统一的表结构
- 通过 `@type` 字段区分具体类型

#### 4.2 关系管理
- 双向关系自动同步
- 级联操作配置
- 懒加载优化

#### 4.3 索引策略
```sql
-- 主要索引
CREATE INDEX idx_element_id ON element(element_id);
CREATE INDEX idx_element_type ON element(type);
CREATE INDEX idx_commit_id ON commit(commit_id);
CREATE INDEX idx_project_id ON project(project_id);
```

## 关键实现细节

### 1. UUID 生成策略
```java
@GenericGenerator(
    name = "UseExistingOrGenerateUUIDGenerator",
    strategy = "org.omg.sysml.jpa.UseExistingOrGenerateUUIDGenerator"
)
```
- 支持客户端指定 UUID
- 未指定时自动生成
- 保证全局唯一性

### 2. JSON-LD 支持
```java
// 提供语义化的 JSON-LD 上下文
@JsonLdAdorner
public class ElementAdorner {
    @Context("http://omg.org/sysml/v2/")
    @Type("sysml:Element")
}
```

### 3. 查询优化
```java
// 使用 Criteria API 构建类型安全的查询
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Element> query = cb.createQuery(Element.class);
Root<Element> root = query.from(Element.class);
query.where(cb.equal(root.get("elementId"), id));
```

### 4. 事务管理
```java
@Transactional
public Element createElement(Element element) {
    // 自动事务边界
    return elementDao.persist(element);
}
```

## 可复用组件

### 1. 元模型实现类
- `/app/org/omg/sysml/metamodel/impl/` - 150+ 个实现类
- 完整实现 SysML v2 规范
- 可直接用于 EMF 或其他框架

### 2. DAO 层模式
- `/app/dao/impl/jpa/` - JPA 数据访问实现
- 通用的 CRUD 操作
- 查询构建器模式

### 3. Service 层
- `/app/services/` - 业务逻辑封装
- 版本控制逻辑
- 事务管理

### 4. JSON Schema
- `/conf/json/schema/` - 完整的 JSON Schema 定义
- 可用于前端验证
- API 文档生成

## 与我们架构的对比

| 方面 | 官方实现 | 我们的架构 |
|------|---------|-----------|
| 持久化 | JPA/Hibernate + PostgreSQL | EMF + CDO + PostgreSQL |
| 模型框架 | Plain Java POJOs | EMF EObject |
| API | REST only | GraphQL + REST |
| 版本控制 | 自定义 Commit 机制 | CDO 内置版本控制 |
| 前端 | 无 | Sirius Web |
| 实时协作 | 无 | CDO + WebSocket |
| 查询能力 | JPA Criteria | OCL + GraphQL |
| 事务管理 | JPA 事务 | CDO 事务 |

## 经验教训

### 优点
1. **完整的规范实现** - 严格遵循 SysML v2 标准
2. **清晰的分层架构** - Controller/Service/DAO 分离
3. **版本控制机制** - 完整的 Git-like 版本管理
4. **扩展性好** - 易于添加新的元模型类型

### 可改进点
1. **缺少实时协作** - 仅支持 REST，无 WebSocket
2. **查询能力受限** - 缺少图形化查询语言
3. **无图形界面** - 仅提供 API，需要额外开发 UI
4. **性能优化空间** - 大模型加载可能较慢

## 总结

SysML v2 API Services 提供了一个**功能完整、架构清晰**的参考实现，特别在以下方面值得学习：

1. **规范的 REST API 设计**
2. **完整的版本控制实现**
3. **清晰的分层架构**
4. **全面的元模型支持**

我们的架构可以**复用其元模型定义和业务逻辑**，但在技术选型上选择了更适合实时协作和图形化建模的 EMF/CDO/Sirius 技术栈。