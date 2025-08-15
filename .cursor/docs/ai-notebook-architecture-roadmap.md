# AI 理解型笔记：Architecture 与 Lean-CDO-Roadmap

更新编号：AR-1

## 一、当前项目现状（基于 Architecture.md 与 Lean-CDO-Roadmap.md）
- 平台定位：面向个人/小团队的 SysML v2 Web 建模平台，技术栈为 Sirius Web（前端）+ Spring Boot GraphQL（API）+ EMF/Lean CDO（模型与持久化）。
- 设计目标：
  - 标准合规（复用官方 `sysml-v2-pilot` 相关能力）
  - 架构简洁（避免过度设计）
  - 现代体验（Web 图形建模）
  - 易扩展（AI 助手等接口预留）
- 关键约束/取舍：
  - Lean CDO 配置精简：禁用分支、审计、锁等高级特性，主打简单与性能。
  - Dev/Prod 存储差异：Dev 用 H2 File Store，Prod 用 PostgreSQL（CDO JDBC）。
  - GraphQL 单点：Resolver/DTO 映射 EMF 对象，订阅用于实时模型变更推送。

## 二、能力分层清单（可执行要点）
- 前端 Sirius Web（React）：
  - Diagram/Table/Tree/Properties/Validation 组件。
  - GraphQL 客户端对接 API；后续可接入 NL-Assistant。
- API 层（Spring Boot GraphQL）：
  - Query/Mutation/Subscription 基本形态明确。
  - Resolver 使用官方 Adapter 将 EObject↔DTO 语义化转换。
  - 校验使用官方 Util；订阅推送模型变更事件。
- 模型与事务（EMF + Lean CDO）：
  - `CDOResourceSet` 单仓库；事务于 Mutation 结束时提交并触发订阅。
  - `EMFModelManager` 提供事务获取/提交/回滚接口；AOP 统一包裹事务。
- 持久化：
  - Dev：H2 嵌入式；Prod：PostgreSQL + 水平映射，表前缀 `CDO_`。
  - 性能优化：参照完整计划加入索引策略与容量基线。
- 部署：
  - Docker Compose（一键本地）、K8s（生产）示例已具雏形。
- 安全：
  - 预留 OAuth2/JWT；CORS 白名单；健康检查与监控（Prometheus）。
- 测试：
  - 单元 + 集成（Testcontainers）；CI Smoke Test 关注 CDO 起服、创建、重启恢复、性能基准。

## 三、路线图与能力映射（MVP1→9周计划）
- MVP1（3 周）：
  - Week1：CDO 起服 + EMF 生成 + Desktop 验证（关键风控点：字段映射/空指针）。
  - Week2：H2Store 接入 + CRUD API 通路（Postman 验收 + 重启恢复）。
  - Week3：需求/结构服务 + 演示脚本（建模→保存→重启→恢复）。
- 9 周总览：
  - 基础设施（CDO/EMF 基线 + 性能基线）
  - 持久化（核心→高级特性）
  - 业务（需求/结构、接口、追溯）与 UI（Sirius Web）
  - API（GraphQL Query/Mutation/Subscription）
  - 优化（性能、错误处理、文档）

## 四、关键风险与对策（结合文档）
- EMF/CDO 集成不稳定：Week1 即验证 Desktop 显示，快速暴露映射问题。
- 性能与容量：Week2 建立基线，后续迭代缓存、批量、DataLoader 方案。
- GraphQL 映射复杂：先 Query 后 Mutation，逐步完成 DTO↔EObject 的覆盖面。
- UI 属性不匹配：引入 Desktop 验证作为早期“黄金标准”。
- 安全：默认开发便捷，生产前需接入鉴权、速率限制、审计与日志留存。

## 五、与官方开源生态的衔接
- 复用官方 `sysml-v2-pilot` 的 Adapter/Util/Factory，确保语义一致性。
- 长期对齐 `sysml-v2-release` 的规范版本；以规范为“单一事实来源”。
- 若需要 REST 互通，可参考 `SysML-v2-API-Services` 的资源模型和 CRUD 语义，作为 GraphQL 语义映射的对照参考。

## 六、下一步建议（可落地）
1) 以最小路径完成 MVP1 的 CI Smoke：CDO 起服→创建 500 元素→重启恢复→计时。
2) 完成 GraphQL 初版 Schema 与 Resolver 的必需路径（需求/结构的 3–5 条关键操作）。
3) 准备开发用 Demo 模型（20 需求 + 50 组件/3 层），贯穿演示与性能 Smoke。
4) 确认生产前安全基线项：JWT、CORS、访问日志、审计与简单速率限制。
5) 将 Desktop 验证与 Web UI 验证并行使用，保证模型字段一致性。

---
该笔记用于对齐理解、控制范围与驱动执行。完成上述建议后，可进入 T6–T9 的成果提交与复盘。
