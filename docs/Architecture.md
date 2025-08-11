# SysML v2 建模平台架构蓝图（纯架构版）

版本：v2（仅架构，不含实现代码）

## 1. 目标与范围
- 目标：以最小复杂度实现基于 SysML v2 的 Web 建模平台，保证标准合规、体验现代、易于扩展。
- 范围：描述技术栈、分层架构、数据/控制流、非功能约束、部署拓扑与演进路线；不包含任何实现代码或具体配置。
- 设计前提：
  - M2（语言/元模型）复用官方 sysml-v2-pilot；本系统聚焦 M1（实例与业务流程）。
  - 最大化复用 pilot 的 Adapter/Util/Factory 与语义模型；接口契约以 GraphQL 为主。

## 2. 技术栈与分层
- 技术栈（确定）：
  - 前端：Sirius Web（React）
  - API 层：Spring Boot GraphQL（Query/Mutation/Subscription）
  - 模型层：EMF + Lean CDO（单 Repository，精简配置：无分支/无审计/无锁）
  - 持久化：Dev=H2 File Store；Prod=PostgreSQL（CDO JDBC Store）
  - 生态复用：sysml-v2-pilot（Adapter/Util/Factory）、sysml-v2-release（规范与模型库）

- 逻辑分层：
  1) 表现层（Sirius Web）
     - 图形/树/表/属性视图；GraphQL 客户端；最小协作能力。
  2) API 层（GraphQL）
     - Resolver 将 DTO ↔ EObject；仅做入参校验与数据映射；不承载业务流程。
  3) 应用服务层（UseCase/Service）
     - 业务编排与事务边界所在；按用例聚合（Requirement/Structure/Constraint/Trace/Report）。
     - 职责：调用领域校验/计算、访问仓储、控制提交、发布模型变更事件。
  4) 领域模型层（EMF/Lean CDO）
     - 使用官方 M2；在 M1 存储实例；提供事务与通知能力。
  5) 持久化层
     - Dev 使用 H2；Prod 使用 PostgreSQL；可观测性/备份/迁移策略在此层定义。

## 3. 数据与控制流（端到端）
1) 用户在 Sirius Web 发起操作（如创建需求/编辑属性）。
2) GraphQL Mutation 到达 API 层；Resolver 进行入参校验与 DTO↔EObject 映射。
3) 调用应用服务层对应 UseCase（@Transactional）：编排校验/计算→仓储读写→提交→发布事件。
4) 提交成功后触发模型变更事件；GraphQL Subscription 将事件推送前端；前端增量刷新。
5) Query 路径：Resolver → UseCase（可选）/Repository → 按类型/层次/拓扑检索与分页结果。

说明：
- M1 数据在 EMF/CDO 中持久化；M2 语义与类型来自 pilot（不在此系统重复定义）。
- 连接合法性/派生值计算/约束校验由“应用服务层”编排，具体算法尽量复用官方工具类。

## 4. 阶段化能力（最小闭环）
- P1 仅需求（优先落地）：
  - 需求 CRUD、层次（derivedFrom）、查询/分页、字段/层次校验。
  - 产出：最小需求树、验收准则字段、检索能力与错误语义。
- P2 结构与参数：
  - Part/Port/Connection 建模；关键参数（电压、功率、质量等）；连接合法性校验。
- P3 约束与追溯：
  - 派生值计算、约束校验（通过/失败明细）；Allocation/Satisfies；覆盖率矩阵。
- P4 订阅与报表：
  - 模型变更事件；最小验证/覆盖率报表；基础导出。

## 5. 生态复用策略
- sysml-v2-pilot：
  - 复用 org.omg.sysml.* 的 Adapter/Util/Factory，以保持语义一致；
  - M2 类型（Definition/Usage、Requirement/Part/Port/Connection/Constraint/Trace 等）不在本系统重复实现。
- sysml-v2-API-Services：
  - 参考其资源语义（Project/Commit/Element/Relationship/Query）以对齐概念；
  - 本系统对外以 GraphQL 为主；必要时可做语义映射层（非必须）。
- sysml-v2-release：
  - 规范与模型库为“单一事实来源”，版本对齐在发布节奏中控制。

## 6. 接口契约与横切关注点
- GraphQL 契约：
  - 内部系统主契约；Query/Mutation/Subscription 三类；分页、过滤、排序统一约定。
  - 错误语义：字段校验、层级循环、跨项目引用、状态迁移非法等分类码。
- 验证与计算：
  - 字段/层级合法性在 API 层快速失败；复杂计算/校验（派生值、约束）由应用服务层协调。
- 并发与一致性：
  - 单仓库事务提交；必要时提供 ETag/版本号以提示客户端更新。

## 11. 目录建议（服务层落位）
- resolver/：GraphQL Resolvers（薄映射/校验，不含业务）
- usecase/：应用服务（编排与事务边界，例如 RequirementUseCase/StructureUseCase/ConstraintUseCase/TraceabilityUseCase/ReportUseCase）
- domain/validation/：层级/连接/约束校验（优先复用 pilot Util）
- domain/calculation/：派生值/度量计算（优先复用 pilot Adapter）
- repo/：仓储抽象（RequirementRepository/ElementRepository）封装 EMF/CDO 访问
- infra/：EMFModelManager、TransactionManager、ModelChangePublisher（Subscription 事件）

## 7. 非功能需求
- 安全：OAuth2/JWT、CORS 白名单、审计日志、最小速率限制；
- 可观测性：应用日志、结构化指标（接口耗时/错误率）、健康检查；
- 性能基线：
  - P1 查询 < 200ms；P2 50+ 组件结构浏览流畅；
  - 批量操作提供合理上限与分片策略。
- 可用性：单实例 CDO；API/前端可水平扩展；备份与恢复演练；
- 数据治理：模型/库版本对齐；数据迁移/导入导出策略。

## 8. 部署拓扑（概念性）
- Dev：单机 Docker Compose（CDO、API、Sirius Web、可选 PostgreSQL）。
- Prod：Kubernetes 单 Pod（CDO+API+Web）或分离部署；CDO 单实例；PostgreSQL 托管；
- 外部依赖：OIDC 提供方、监控/日志堆栈（Prometheus/ELK 等）。

## 9. 风险与缓解
- EMF/CDO 集成风险：以 Desktop/小样模型早验证；
- 语义对齐风险：以 sysml-v2-release 为准，回归关键用例；
- 性能风险：建立 P2 基线后持续压测与优化（缓存、批量、DataLoader）；
- 安全风险：生产前开启鉴权/授权与审计，联调 OIDC。

## 10. 术语与约定
- Definition/Usage：保持官方二分；针对 M1 最小闭环在文档中“端口”以 Port 指代 PortUsage（挂在实例上的端口），减少表达负担；需要抽象复用时回到 PortDefinition 层。
- M2 复用：不在本系统重复定义语言或元模型；以官方 pilot 为准。

—— 本蓝图仅用于架构约束与协作对齐，后续实现以阶段化（P1→P4）推进并据此更新契约与验收标准。


