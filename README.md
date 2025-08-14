# SysML v2 建模平台

基于 EMF/CDO + GraphQL + Sirius Web 的 SysML v2 Web建模平台

## 🚀 快速启动

```bash
# 1. 克隆仓库
git clone <repository-url>
cd sysml2

# 2. 启动开发环境
./scripts/dev/start.sh

# 3. 验证服务
curl http://localhost:8080/health
# 应返回: {"status":"UP"}

# 4. 访问GraphQL Playground (仅开发环境)
open http://localhost:8080/graphql

# 5. 访问Sirius Web
open http://localhost:3000
```

## 📁 项目结构

```
sysml2/
├── Architecture.md                 # 架构设计（v3 MECE版）
├── agile_traceability_matrix.yaml  # 需求追踪矩阵（v5）
├── api_contract.graphqls          # API契约（单一真相源）
├── ADR.md                          # 架构决策记录
├── README.md                       # 本文件
│
├── server/                         # 后端代码
│   ├── api/graphql/               # GraphQL层
│   ├── domain/                    # 领域模型
│   ├── application/               # 应用服务
│   └── infrastructure/            # 基础设施
│
├── sirius/                        # Sirius配置
│   └── modelers/                  # 视图定义
│
└── scripts/                       # 脚本工具
    ├── dev/                       # 开发脚本
    └── test/                      # 测试脚本
```

## 🏗️ 技术栈

- **后端**: Spring Boot 3.x + Java 17
- **API**: GraphQL (Spring GraphQL)
- **建模**: EMF + Lean CDO
- **UI**: Sirius Web
- **数据库**: H2 (dev) / PostgreSQL (prod)
- **M2复用**: sysml-v2-pilot

## 🎯 核心原则

1. **MECE**: 领域职责无重叠且完整
2. **DAG**: 依赖无环
3. **契约先行**: GraphQL Schema驱动
4. **最小可用**: 增量交付

## 📊 开发阶段

| 阶段 | 目标 | 状态 |
|------|------|------|
| Foundation | 技术栈验证 | 🚧 进行中 |
| P1-需求 | 需求管理 | ⏳ 待开始 |
| P2-结构 | 结构建模 | ⏳ 待开始 |
| P3-约束 | 约束分析 | ⏳ 待开始 |
| P4-追溯 | 追溯管理 | ⏳ 待开始 |
| P5-输出 | 报表导出 | ⏳ 待开始 |

## 🧪 测试

```bash
# 单元测试
./gradlew test

# 集成测试
./gradlew integrationTest

# 性能测试
./gradlew performanceTest --dataset=small

# 架构测试（循环依赖检查）
./gradlew architectureTest
```

## 📈 性能基准

| 数据集 | 元素数 | 查询P50 | 变更P50 |
|--------|--------|---------|---------|
| Small | 100 | <100ms | <200ms |
| Medium | 1000 | <200ms | <400ms |
| Large | 10000 | <500ms | <800ms |

## 🔍 关键端点

- **健康检查**: GET `/health`
- **GraphQL**: POST `/graphql`
- **指标**: GET `/metrics`
- **Sirius**: `http://localhost:3000`

## 📝 核心文档

1. [架构设计](Architecture.md) - 系统架构和设计决策
2. [需求矩阵](agile_traceability_matrix.yaml) - Epic/Story/Requirement追踪
3. [API契约](api_contract.graphqls) - GraphQL Schema定义
4. [决策记录](ADR.md) - 架构决策记录
5. [开发指南](docs/development.md) - 详细开发文档

## 🛠️ 开发工具

```bash
# GraphQL Schema验证
graphql-schema-linter api_contract.graphqls

# 生成TypeScript类型
graphql-codegen

# 代码格式化
./gradlew spotlessApply

# 依赖检查
./gradlew dependencyCheckAnalyze
```

## 🐛 问题排查

### CDO无法启动
```bash
# 检查端口占用
lsof -i:2036

# 清理CDO数据
rm -rf data/cdo/*
```

### GraphQL Schema错误
```bash
# 验证Schema
./scripts/dev/validate-schema.sh

# 查看Schema快照差异
git diff schema.snapshot.graphql
```

### 性能问题
```bash
# 开启详细日志
export LOG_LEVEL=DEBUG

# 查看DataLoader指标
curl http://localhost:8080/metrics | grep dataloader
```

## 🤝 贡献

1. 遵循MECE原则
2. 保证无循环依赖
3. 所有Mutation使用Payload模式
4. 提交前运行测试

## 📄 许可

[License Type]

---

**快速链接**:
- [CI/CD](https://github.com/...)
- [Issue Tracker](https://github.com/...)
- [Wiki](https://github.com/...)