# 实施计划（v1）
> 基于《架构蓝图 v3》与《需求矩阵 v5（MECE）》；以**契约先行、最小可用、逐波推进**为原则。

---
## 0. 总体策略
- **波次（Waves）+ 冲刺（Sprints 2w）**：按 P0→P1→P2→P4→P5→P6→P7→P8 组织；每波 1–2 个冲刺。
- **契约先行**：先挂 GraphQL 模块 & 快照，再填 resolver/存储；Sirius 先绑定，视图随域推进。
- **MECE 边界**：
  - REQ：仅需求域；跨域链路移交 TRACE。
  - STRUCT：拓扑/连接合法性；公式/阈值移交 ANALYSIS。
  - PROP：属性/单位，不做计算。
  - ANALYSIS：派生/约束求值与回写，不建链路。
  - TRACE：链路与覆盖率，只读消费分析结果。
  - EVENTS/REPORT/EXPORT：订阅/报表/导出入，不承载领域规则。
- **DAG 无环**：各 EPIC 的 `depends_on_epics` 固化在母档；任何写回跨域字段的需求 **需开 ADR**。

---
## 1. 里程碑与时间轴（建议）
> 以 2 周为一个 Sprint，可按人力缩放；如并行能力强，部分波次可交错推进（注意依赖）。

| Wave | Sprint | 目标里程碑 | 退出门槛（Exit Criteria） |
|---|---|---|---|
| **P0 Foundation** | S1 | /graphql 入口与核心约定；EMF/CDO/Sirius 绑定；CI 门禁 | `/health/*` 全绿；`/graphql` 探活；Sirius 会话落盘；Schema 快照上线 |
|  | S2 | pilot M2+adapters round‑trip；CDO 提交-重启恢复；sysml.library 导入 | XMI/JSON 往返等价；提交后重启数据不丢；library 可查询 |
| **P1 REQ** | S3 | `requirements.graphqls` 上线（快照）；Create/Query + 层级 DAG 校验 | 需求创建/查询可用；环检测生效；错误码表对齐 |
|  | S4 | 关系/审计/分页性能；Sirius 需求树/表最小视图 | 1k 数据 P50<200ms；DataLoader 无 N+1；树/表可编辑 |
| **P2 STRUCT** | S5 | `structure.graphqls` 上线；Part/Port 基线 | 节点/端口 CRUD；端口方向/种类受控 |
|  | S6 | 连接合法性 + 结构图（Sirius） | 非法连接被拒并有 UI 装饰；批量操作性能达标 |
| **P4 PROP** | S7 | 属性/单位/模板最小实现 | 单位一致性校验；模板应用/撤销 |
| **P5 ANALYSIS** | S8 | Formula 模块 + 执行与回写；样例派生（endurance/current） | 解析/执行/回写含 provenance；样例断言在容差 |
| **P6 TRACE** | S9 | satisfy/verify/allocation + 覆盖率矩阵 | 链路正反遍历；覆盖率 JSON/CSV；不回写 REQ |
| **P7 EVENTS/REPORT/EXPORT** | S10 | 订阅 + 报表 + 导出/导入往返 | 订阅 P90<100ms；报表 P90<2s；导出→导入语义等价 |
| **P8 SCENARIOS** | S11 | UAV 场景模板（R/F/C）与验收脚本 | 模板驱动各域用例跑通；非端到端实现 |

---
## 2. 波次分解与交付清单（与需求矩阵对齐）
### P0 — Foundation（S1–S2）
**目标**：只铺地基最小集。
- EP-BOOTSTRAP：一键构建 `scripts/dev.sh`；CI 门禁 `.github/workflows/ci.yml`；/health 汇总。
- EP-API-GQL：`core.graphqls`（标量/分页/错误/`ok`）；健康探针；DataLoader 指标；Schema 快照测试。
- EP-M2-PILOT：EPackage 注册；Factory 检查；XMI/JSON round‑trip IT。
- EP-CDO：Dev=H2 启动脚本与健康检查；提交-重启恢复 IT；Tx 边界单测。
- EP-LIB：`import-sysml-library.sh`；规范包查询 IT。
- EP-SIRIUS：运行时绑定与会话持久化；`requirements.modeler.json`/`structure.modeler.json` 占位。
- EP-NFR：结构化日志（traceId/spanId）；错误码注册；DEV/PROD 鉴权开关。

**验收（DoD）**：
- [ ] `/health/*` 全部 `UP`；
- [ ] `/graphql` + GraphiQL（dev）可用；
- [ ] `schema snapshot` 首次基线生成；
- [ ] XMI/JSON 往返等价；
- [ ] CDO 重启不丢；library 可查；
- [ ] Sirius 打开表示、编辑后持久化至 CDO。

### P1 — Requirements（S3–S4）
**交付**：
- `requirements.graphqls` 上线（Query/Mutation/Subscription 只放最小集）。
- 需求 CRUD、层级 DAG 校验、删除策略（REF_IN_USE）、关系校验、审计字段。
- 查询/过滤/分页（DataLoader 无 N+1，1k P50<200ms）。
- Sirius 需求树/表（拖拽防环、内联编辑）。
**DoD**：
- [ ] `US-REQ-API-000` 快照通过；
- [ ] `RQ-REQ-*` 单测≥90%覆盖；
- [ ] E2E 从空库→最小需求树脚本跑通；
- [ ] 需求视图可读写、错误装饰正确。

### P2 — Structure（S5–S6）
**交付**：
- `structure.graphqls` 模块；Part/Port CRUD；Port 枚举受控；Connection 规则与错误码（CONNECTION_INVALID）。
- Sirius 结构图：节点/端口/边 + palette；非法连接 UI 装饰；批量创建性能。
**DoD**：
- [ ] 非法连接被拒且有错误码；
- [ ] 200 节点/300 边图，首开 P50<1.2s；批量 50 节点 <2s。

### P4 — Property & Units（S7）
**交付**：
- 属性类型/单位/量纲，基本换算；属性模板应用/撤销；与 STRUCT/REQ 字段解耦。
**DoD**：
- [ ] 非法单位不通过；模板回滚干净。

### P5 — Analysis/Parametrics（S8）
**交付**：
- Formula 计算模块（表达式解析、输入收集、单位检查、执行、回写、provenance）。
- 派生样例：`endurance/totalCurrent`；约束示例：`endurance≥20min`、`totalCurrent≤maxCurrent`。
**DoD**：
- [ ] 容差断言通过；
- [ ] 回写记录来源（moduleId/version/timestamp）。

### P6 — Traceability（S9）
**交付**：
- satisfy/verify/refine/allocation 关系模型与 API；覆盖率矩阵（JSON/CSV）。
**DoD**：
- [ ] 链路正反遍历正确；
- [ ] 覆盖率 P90<1.5s；
- [ ] 不回写 REQ（只读消费）。

### P7 — Events/Report/Export（S10）
**交付**：
- Subscription：模型变更事件、局部刷新；报表：验证/覆盖率摘要；导出/导入往返等价。
**DoD**：
- [ ] 订阅端到端 P90<100ms；
- [ ] 报表 P90<2s；
- [ ] 导出→导入语义等价（忽略 UUID）。

### P8 — Scenario Packs（S11）
**交付**：
- UAV 场景模板（R/F/C）；示例数据与验收脚本；**非端到端实现**（端到端另开 EPIC）。
**DoD**：
- [ ] 模板可驱动各域最小用例；
- [ ] 演示脚本跑通。

---
## 3. 角色与并行度
- **Platform（2人）**：P0–P1 的 API/NFR/CDO/M2/Sirius 绑定与 CI。
- **Backend（2–3人）**：按 EPIC 推进 resolver/仓储/校验/性能。
- **Modeling/UI（1–2人）**：Sirius modeler 与 UI 装饰；与 GraphQL 表格页联动。
- **QA（1人）**：测试金字塔、schema 快照、E2E 脚本与性能基准。
> 人力不足时：优先保证 **API 契约 + 校验规则 + 性能基线**，Sirius 视图以最小化为准。

---
## 4. 质量与度量（测试金字塔）
- **Unit（占 60%）**：规则/解析/校验/适配器；覆盖率≥90%。
- **Integration（占 30%）**：GraphQL + CDO；DataLoader 指标；往返等价。
- **E2E（占 10%）**：演示脚本（空库→最小树 / 非法连接 / 约束求值 / 导入导出）。
- **性能基线**：查询 1k P50<200ms；订阅 P90<100ms；报表 P90<2s；结构图首开 P50<1.2s。
- **可观测性**：`/health`、`/metrics`（含 nPlusOneCount）、结构化日志 traceId/spanId。

---
## 5. 文档与“单一真相源”
- **架构文档**：`/docs/architecture/architecture.md`（已对齐蓝图）。
- **API 文档**：SDL = 真相；CI 生成 `docs/api/reference/`；示例即测试（`docs/api/examples/**`）。
- **UI/Modeler**：`sirius/modelers/*.json` 为真相；文档仅放映射表与装饰规则。
- **ADR（关键变更）**：GraphQL 选型、Sirius 对接、M2 复用、CDO 策略、错误模型。

---
## 6. 风险与缓解
- **隐性回写导致环**：TRACE→REQ；**缓解**：只读消费，状态不回写。
- **N+1 或 schema 漂移**：**缓解**：DataLoader 指标 + schema 快照 + diff。
- **Sirius 与域模型错位**：**缓解**：每域一条 modeler 占位 Req，随域推进配置。
- **性能被忽视**：**缓解**：每域设 P50/P90 门槛与基准数据集。

---
## 7. 验收清单（全局 DoD）
- [ ] 每个 EPIC 的 **契约快照**存在并通过 CI。
- [ ] **追踪矩阵**完整（Epic→Story→Req→Test→CodePath）。
- [ ] **演示脚本**至少 3 条（需求树、结构连接、约束求值/报表/导出导入）。
- [ ] **文档**四件套可用（架构/参考/示例/变更）。

---
## 8. 立即行动（Week 1）
1) 初始化 P0 仓库脚手架与 CI（BOOTSTRAP/NFR）。
2) 提交 `core.graphqls` + `schema snapshot`（API-GQL）。
3) 接入 pilot M2，跑通最小 Factory 单测（M2-PILOT）。
4) 启动 CDO Dev，完成健康探针（CDO）。
5) 放置 `sirius/modelers/*.json` 占位并完成会话持久化（SIRIUS）。

> 完成以上，Week 2 进入 P1（REQ）的契约与最小能力实现。



---
## Sirius 视图覆盖矩阵（显式表达）
> 说明：Sirius 的“平台能力”在 EP‑SIRIUS 交付；各领域 EPIC 仅声明本域需要的表示（representation）与占位文件路径，避免耦合与重复。

| EPIC | 视图（Representation） | 文件/占位 | 关键交互/规则 | 依赖 |
|---|---|---|---|---|
| **EP‑SIRIUS** | 运行时/会话（基座）; 图层/条件样式; 验证标记; 订阅增量刷新; 图导出 | N/A | 健康检查 `/health/sirius=UP`；局部刷新；导出 SVG/PNG | CDO, NFR |
| **EP‑REQ** | **Tree / Table** | `sirius/modelers/requirements.modeler.json` | 新建/重命名/删除；**拖拽防环**（显示 `REQ_CYCLE_DETECTED` 装饰）；表内联编辑/过滤 | EP‑SIRIUS, EP‑API‑GQL |
| **EP‑STRUCT** | **Diagram（Part/Port/Connection）** | `sirius/modelers/structure.modeler.json` | palette：新建部件/端口/连接；**非法连接装饰**（`CONNECTION_INVALID`）；批量创建性能 | EP‑SIRIUS |
| **EP‑PROP** | **Form（属性表单）** |（复用通用表单配置）| EAttribute 可编；EReference 选择器；**派生/计算字段只读**；单位后缀显示 | EP‑SIRIUS |
| **EP‑ANALYSIS** |（无新增表示）| — | 复用“验证标记/条件样式”在现有图/表上呈现计算/约束结果 | EP‑SIRIUS, EP‑PROP |
| **EP‑TRACE** |（可选）覆盖率表（后置）| （后置） | 初期以 JSON/CSV 报表为主；如需表视图再追加占位 | EP‑SIRIUS, EP‑API‑GQL |
| **EP‑EVENTS** |（无表示）| — | 复用“订阅增量刷新”，驱动当前打开视图 | EP‑SIRIUS |
| **EP‑SCENARIOS** |（无表示）| — | 仅提供场景模板与脚本，不直接产出图形表示 | — |

> 注：若未来恢复“功能切面（Function）”，则在 EP‑SIRIUS 中启用 **Function Diagram**（节点/槽/流、DAG 装饰），并在该 EPIC 声明 `modelers/function.modeler.json` 占位。

