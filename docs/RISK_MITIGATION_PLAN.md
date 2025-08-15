# CDO集成风险缓解计划

## 已识别的风险点

### 1. GraphQL CDO API未实现 ⚠️
**风险**: API契约已定义但缺少实现
**影响**: 前端无法使用CDO功能
**缓解措施**: 
- ✅ 已创建 `/server/src/main/java/com/sysml/platform/api/CDOGraphQLResolver.java`
- ✅ 已创建 `/server/src/main/resources/graphql/cdo.graphqls`
- TODO: 集成业务层服务

### 2. 测试依赖外部PostgreSQL 🔧
**风险**: 测试依赖localhost:5432，无数据库时失败
**影响**: CI/CD环境测试不稳定
**缓解措施**:
- ✅ 已创建 `BaseIntegrationTest` 基类统一使用Testcontainers
- TODO: 迁移现有测试继承BaseIntegrationTest
- TODO: 移除所有硬编码的localhost:5432

### 3. 数据源配置不一致 🔄
**风险**: 多处配置源导致连接失败
**影响**: 环境切换时出错
**缓解措施**:
- ✅ 已创建 `UnifiedDataSourceConfig` 统一配置
- 优先级: spring.datasource.* > DATABASE_* > 默认值
- TODO: 更新所有测试使用统一配置

### 4. 代码重复 📁
**风险**: 两个位置有CDO实现
**影响**: 代码漂移，维护困难

| 文件 | 位置 | 建议 |
|------|------|------|
| CDOServerManager.java | `/server/src/main/java/...` | ✅ 保留（主实现） |
| CDOServerManager.java | `/sysml-platform/backend/src/...` | ❌ 删除或标记过时 |

## 实施计划

### 第一阶段：立即修复（P0）
1. [x] 实现GraphQL CDO Resolver
2. [x] 创建统一测试基类
3. [x] 创建统一数据源配置

### 第二阶段：测试迁移（P1）
```bash
# 需要迁移的测试类
- CDODebugTest → extends BaseIntegrationTest
- CDOStorageTest → extends BaseIntegrationTest  
- HealthControllerIT → extends BaseIntegrationTest
- ModelControllerIT → extends BaseIntegrationTest
```

### 第三阶段：代码清理（P2）
```bash
# 标记过时代码
@Deprecated(since = "2025-08-14", forRemoval = true)
// sysml-platform/backend/src/.../CDOServerManager.java

# 添加README说明
/sysml-platform/backend/README.md
> ⚠️ CDO实现已迁移到 /server 目录
```

## 配置示例

### application.yml统一配置
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://${DATABASE_HOST:localhost}:${DATABASE_PORT:5432}/${DATABASE_NAME:sysml_db}}
    username: ${SPRING_DATASOURCE_USERNAME:${DATABASE_USER:sysml_user}}
    password: ${SPRING_DATASOURCE_PASSWORD:${DATABASE_PASSWORD:sysml_password}}

datasource:
  unified: true  # 启用统一数据源

cdo:
  enabled: ${CDO_ENABLED:true}
  repository:
    name: ${CDO_REPOSITORY_NAME:sysml-repo}
```

### 测试配置示例
```java
@SpringBootTest
class MyIntegrationTest extends BaseIntegrationTest {
    // 自动获得Testcontainers管理的PostgreSQL
    // 无需@TestPropertySource
    // 无需硬编码localhost:5432
}
```

## 验证检查清单

- [ ] 所有测试使用Testcontainers
- [ ] 无硬编码的localhost:5432
- [ ] 数据源用户名统一为sysml_user
- [ ] CDO GraphQL端点可访问
- [ ] 删除重复的CDO实现

## 风险矩阵

| 风险 | 概率 | 影响 | 缓解状态 |
|------|------|------|----------|
| API未实现 | 高 | 高 | ✅ 已缓解 |
| 测试不稳定 | 中 | 中 | 🔧 进行中 |
| 配置冲突 | 中 | 低 | ✅ 已缓解 |
| 代码重复 | 低 | 低 | 📋 计划中 |

---
*最后更新: 2025-08-14*