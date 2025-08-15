## SysML v2 开源生态总览（当前仓库快照）

本概览基于本仓库 `opensource/` 目录内的五大子项目与相关资料，聚焦“组件角色—提供能力—已知边界—集成关系—落地建议”。

### 生态与组件地图
- **SysML-v2-API-Services（后端 REST 服务）**
  - 基于 Play Framework（Java/SBT）实现的 SysML v2 API 原型服务，提供 `Project / Branch / Tag / Commit / Element / Relationship / Query` 等核心资源的 REST/HTTP 接口，支持 JSON 与 JSON-LD 输出，内置 Swagger 文档路由 `/docs`。
  - 数据库：PostgreSQL（建议使用 Docker 启动）。
  - 关键文件：`conf/routes`（路由）、`app/controllers/*Controller.java`（控制器）、`conf/application.conf`（配置）、`public/docs`（OpenAPI/Swagger 静态页面与规范）。

- **SysML-v2-API-Java-Client（Java 客户端 SDK）**
  - 由 OpenAPI 生成的 Java 客户端，覆盖与服务端一致的资源模型与方法，便于 Java 应用集成调用。
  - 关键文件：`api/openapi.yaml`（接口定义）、`docs/*.md`（端点与模型文档）、`src/main/java`（SDK 源码）。

- **SysML-v2-API-Cookbook（Jupyter 配方）**
  - 一组 Jupyter Notebook，展示 API 的典型用法与调用模式，包括项目/提交/分支/标签操作、元素增删改查、递归拥有元素遍历、查询等。
  - 关键文件：`*.ipynb`（配方示例）、`README.md`（配方说明）。

- **sysml-v2-pilot（编辑器与内核原型）**
  - Eclipse/Xtext 驱动的 KerML/SysML v2 文本编辑器、PlantUML 可视化扩展与 Jupyter Kernel 等原型实现；可在 Eclipse 中加载 `kerml`、`sysml`、`sysml.library` 项目进行编辑与构建。
  - 关键文件：各 `org.omg.*` 目录（Xtext/Java 实现、插件与特性）、`README.adoc`（安装与构建说明）。

- **sysml-v2-release（规范与示例发布仓库）**
  - 包含最新规范文档（PDF）、示例模型与模型库，以及 Eclipse/Jupyter 安装器说明。
  - 规范状态（摘自 README）：OMG 已于 2025-06-30 正式通过 KerML 1.0、SysML 2.0、Systems Modeling API & Services 1.0，正式规范预计 2025 年内发布；当前提供 Beta 版本链接与发布节奏说明。

### 能力与接口覆盖（以 API-Services 与 Java-Client 为主）
- **资源族**：
  - **Project**：创建、查询、更新、删除；分页参数 `page[after] / page[before] / page[size]`。
  - **Branch / Tag**：项目内分支与标签的增删查。
  - **Commit**：按项目创建提交（可带 `branchId`），查询提交列表、单提交、提交变更（changes）。
  - **Element**：按 `projectId + commitId` 列表/单项获取元素、根元素；可按 `qualifiedName` 查询；支持 `excludeUsed` 过滤；提供 `projectUsage` 能力（JSON 模式）。
  - **Relationship**：按相关元素与方向参数查询关系。
  - **Query**：查询定义的增删查，以及按定义或 ID 执行并获取结果。
- **表示与协议**：
  - 支持 `application/json` 与 `application/ld+json`（JSON-LD）；部分控制器在 JSON-LD 路径上仍标记为 TODO/未实现。
  - 支持基于 UUID 的分页与 Link 导航（见控制器分页工具与路由）。
  - Java 客户端与服务端的资源/端点一一对应，便于直接对接。

### 已知边界与注意事项
- **JSON-LD 完整度**：`CommitController.getCommitByProjectAndId`、`getChangesByProjectAndCommit` 等在 JSON-LD 模式下返回 NOT_IMPLEMENTED，表明 JSON-LD 表达的覆盖尚未完全。
- **鉴权**：Java 客户端 README 标注“所有端点不要求鉴权”；服务端默认配置亦未启用鉴权/授权策略。生产部署需补充认证与访问控制。
- **版本一致性**：Java 客户端历史版本要求 Java 1.7+，服务端依赖 JDK 11/SBT/Play；建议在集成工程中统一到 LTS JDK 版本，并关注 OpenAPI 规范与实现的版本匹配。
- **性能与容量**：大模型/深层所有者遍历、图关系查询需关注分页与过滤参数的使用；建议结合数据库索引与服务端批量/流式响应策略优化。

### 组件集成关系（建议参考路径）
- API-Services 提供后端存储与 API 入口；
- API-Java-Client 作为 SDK 直接调用 API-Services；
- API-Cookbook 展示 Notebook 方式调用 API-Services 的实践配方；
- sysml-v2-pilot 提供 Eclipse/Xtext 编辑体验与 Jupyter Kernel，模型可通过 API 入库或从库中导出；
- sysml-v2-release 提供规范、示例与安装器，作为规范状态与示例模型的“单一事实来源”。

### 落地与运维建议
- **安全**：启用鉴权（OAuth2/OIDC 或反向代理鉴权）、CORS 白名单与速率限制；更新 `play.http.secret.key`；隔离管理接口；开启访问日志与审计。
- **数据层**：为高频查询字段加索引；规范变更需要数据迁移策略与版本化；建议加 CDC/备份恢复流程。
- **API 设计**：补齐 JSON-LD 输出；对批量/流式接口给出明确限制与错误语义；完善错误模型（含可观测性 ID）。
- **一致性与测试**：以 OpenAPI 为契约源，生成多语言 SDK；建立端到端与契约测试；对 Notebook 配方进行持续验证。
- **规范演进**：跟踪 `sysml-v2-release` 的正式发布版本，确保实现与 Beta4/Beta3 链接所指版本保持一致；定期回归关键用例（提交、派生关系、查询、库引用）。

### 快速入口（仓内定位）
- 服务端路由与配置：`opensource/SysML-v2-API-Services/conf/routes`、`conf/application.conf`
- 控制器示例：`app/controllers/ProjectController.java`、`ElementController.java`、`CommitController.java`
- OpenAPI 定义：`opensource/SysML-v2-API-Java-Client/api/openapi.yaml`
- 客户端端点与模型文档：`opensource/SysML-v2-API-Java-Client/docs/`
- Jupyter 配方：`opensource/SysML-v2-API-Cookbook/*.ipynb`
- Pilot 安装与构建：`opensource/sysml-v2-pilot/README.adoc`
- 规范与发布说明：`opensource/sysml-v2-release/README.md`、`doc/*.pdf`

### 技术债务与后续路线（建议）
- 完成 JSON-LD 输出路径的统一与覆盖测试。
- 为生产化补齐鉴权/授权、审计与速率限制。
- 将服务端 OpenAPI 与 SDK 发布流水线化，提供多语言 SDK。
- 增强查询能力（组合条件、分页游标、排序与筛选统一约定）。
- 增加大规模模型的性能基准与容量上限文档。

—— 本文档由仓库内代码、配置与文档整理而成，用于团队快速认知与实施参考。


