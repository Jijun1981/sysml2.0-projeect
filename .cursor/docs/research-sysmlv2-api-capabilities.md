# 研究：SysML v2 API 能力与边界

研究范围
- 服务端：`opensource/SysML-v2-API-Services`（Play/Java/SBT，PostgreSQL）
- 客户端：`opensource/SysML-v2-API-Java-Client`（OpenAPI 生成）
- 用法：`opensource/SysML-v2-API-Cookbook`（Jupyter 配方）

能力概览
- 核心资源：Project、Branch、Tag、Commit、Element、Relationship、Query
- 关键操作：
  - Project/Branch/Tag：CRUD（按项目范围）
  - Commit：创建（可指定 `branchId`）、查询、变更列表/单个变更
  - Element：按 project+commit 查询列表/单项、根元素、按 `qualifiedName` 查询；`excludeUsed` 过滤；`projectUsage`（JSON 形态）
  - Relationship：按 relatedElementId 与方向查询
  - Query：定义的增删查，按定义或 ID 执行
- 表达：`application/json` 与 `application/ld+json`（JSON-LD）。分页支持 `page[after] / page[before] / page[size]`。

边界与改进点
- JSON-LD：部分控制器在 JSON-LD 模式下返回 NOT_IMPLEMENTED，需要补齐与验证（例如提交与变更相关接口）。
- 安全：默认未启用鉴权；生产化需补充认证/授权、速率限制、审计等。
- 版本：客户端与服务端 JDK 要求不同；建议统一到 LTS 并以 OpenAPI 为契约源生成 SDK。

引用来源（仓内）
- 路由与控制器：`opensource/SysML-v2-API-Services/conf/routes`、`app/controllers/*`
- OpenAPI：`opensource/SysML-v2-API-Java-Client/api/openapi.yaml`、`docs/*.md`
- 配方：`opensource/SysML-v2-API-Cookbook/*.ipynb`
