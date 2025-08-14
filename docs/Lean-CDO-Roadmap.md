# Lean CDO 技术实施路线

## 1. 核心技术栈

```
CDO (Connected Data Objects) 4.x
├── 模型存储层
│   ├── EMF Resource管理
│   ├── 懒加载机制
│   └── 变更通知系统
├── 网络传输层
│   ├── Net4j协议
│   ├── TCP/HTTP传输
│   └── 二进制序列化
└── 持久化层
    ├── DBStore (PostgreSQL)
    ├── HibernateStore
    └── MEMStore (开发用)
```

## 2. 精简配置策略

### 2.1 禁用非必要特性
```properties
# cdo-server.xml 精简配置
supporting.branches=false      # 禁用分支
supporting.audits=false        # 禁用审计
supporting.units=false         # 禁用工作单元
supporting.login=false         # 禁用登录认证
idGeneration.location=STORE    # ID在服务端生成
```

### 2.2 性能优化参数
```properties
# 内存与缓存
cache.current.lru.capacity=10000000    # LRU缓存10M对象
cache.revised.lru.capacity=1000000     # 历史缓存1M对象
commit.conflict.resolver=NONE          # 禁用冲突解决
query.cache.size=1000                  # 查询缓存
```

### 2.3 PostgreSQL优化
```sql
-- 索引策略
CREATE INDEX idx_cdo_objects_class ON cdo_objects(eclass);
CREATE INDEX idx_cdo_objects_container ON cdo_objects(econtainer);
CREATE INDEX idx_cdo_references_source ON cdo_references(cdo_source);

-- 连接池配置
max_connections = 200
shared_buffers = 256MB
effective_cache_size = 1GB
```

## 3. 分阶段实施

### Phase 1: 最小可行CDO (Week 1)
```java
// 1. 嵌入式CDO服务器
public class EmbeddedCDOServer {
    public void start() {
        IStore store = MEMStore.createMEMStore();
        IRepository repository = CDOServerUtil.createRepository(
            "sysml", store, new HashMap<>()
        );
        
        Net4jUtil.getAcceptor(IPluginContainer.INSTANCE, "tcp", "0.0.0.0:2036");
    }
}

// 2. 客户端连接
CDOSession session = CDOUtil.openSession("tcp://localhost:2036/sysml");
CDOTransaction transaction = session.openTransaction();
```

### Phase 2: 持久化集成 (Week 2)
```java
// PostgreSQL DBStore配置
IDBAdapter dbAdapter = new PostgreSQLAdapter();
IDBConnectionProvider connectionProvider = dbAdapter.createConnectionProvider(dataSource);
IStore store = CDODBUtil.createStore(
    mappingStrategy,
    dbAdapter,
    connectionProvider
);
```

### Phase 3: 模型适配 (Week 3)
```java
// EMF模型注册
CDOPackageRegistry.INSTANCE.put(SysmlPackage.eINSTANCE);
CDOPackageRegistry.INSTANCE.put(KermlPackage.eINSTANCE);

// Lazy Loading配置
CDOView view = session.openView();
view.options().setRevisionPrefetchingPolicy(
    CDOUtil.createRevisionPrefetchingPolicy(10) // 预取10个对象
);
```

## 4. 性能基准

### 4.1 目标指标
| 操作 | 目标性能 | 测试场景 |
|------|----------|----------|
| 创建元素 | < 5ms | 单个RequirementDefinition |
| 批量创建 | < 1ms/个 | 1000个元素批量提交 |
| 查询响应 | < 50ms | 200元素全量查询 |
| 更新通知 | < 10ms | 属性变更广播 |
| 内存占用 | < 512MB | 10000个活动对象 |

### 4.2 压力测试
```java
@Test
public void stressTest() {
    CDOTransaction tx = session.openTransaction();
    CDOResource resource = tx.createResource("/test");
    
    long start = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        PartDefinition part = SysmlFactory.eINSTANCE.createPartDefinition();
        part.setName("Part_" + i);
        resource.getContents().add(part);
        
        if (i % 100 == 0) {
            tx.commit(); // 批量提交
        }
    }
    
    long duration = System.currentTimeMillis() - start;
    assert duration < 10000; // 10秒内完成
}
```

## 5. 关键技术点

### 5.1 懒加载策略
```java
// 按需加载大模型
CDORevisionPrefetchingPolicy policy = new CDORevisionPrefetchingPolicy() {
    public int getPrefetchDepth() {
        return 2; // 只预取2层
    }
};
```

### 5.2 变更通知机制
```java
// 实时协作支持
transaction.addTransactionHandler(new CDOTransactionHandler() {
    public void committedTransaction(CDOTransaction tx, CDOCommitContext ctx) {
        // GraphQL订阅推送
        webSocketHandler.broadcast(ctx.getNewObjects());
    }
});
```

### 5.3 查询优化
```java
// 使用CDO Query避免全量加载
CDOQuery query = view.createQuery("sql", 
    "SELECT * FROM RequirementDefinition WHERE name LIKE ?");
query.setParameter(0, "REQ-%");
query.setMaxResults(100);
List<RequirementDefinition> results = query.getResult();
```

## 6. 监控指标

### 6.1 JMX监控
```java
// CDO内置监控
CDOServerUtil.addMonitoringSupport(repository);

// 关键指标
- repository.getRevisionManager().getCache().getCurrentRevisions()
- repository.getSessionManager().getSessions().length
- repository.getLockingManager().getLocks().size()
```

### 6.2 性能追踪
```properties
# 启用CDO追踪
org.eclipse.emf.cdo.server.LEVEL=DEBUG
org.eclipse.emf.cdo.server.db.LEVEL=TRACE
```

## 7. 故障处理

### 7.1 常见问题
| 问题 | 原因 | 解决方案 |
|------|------|----------|
| CommitConflictException | 并发修改 | 实现乐观锁重试 |
| ObjectNotFoundException | 懒加载失败 | 增加预取深度 |
| OutOfMemoryError | 缓存过大 | 调整LRU容量 |

### 7.2 恢复策略
```java
// 自动重连机制
public class CDOSessionManager {
    @Retryable(maxAttempts = 3)
    public CDOSession getSession() {
        if (session == null || !session.isClosed()) {
            session = openNewSession();
        }
        return session;
    }
}
```

## 8. 迁移路径

### 从JPA迁移
```java
// 阶段1: 双写
@Transactional
public void save(Element element) {
    jpaRepository.save(element);     // 写JPA
    cdoRepository.save(element);     // 写CDO
}

// 阶段2: 读切换
public Element find(String id) {
    return cdoRepository.find(id);   // 从CDO读
}

// 阶段3: 下线JPA
```

## 9. 部署架构

```yaml
# docker-compose.yml
services:
  cdo-server:
    image: sysml/cdo-server:4.x
    ports:
      - "2036:2036"
    environment:
      - DB_HOST=postgres
      - CACHE_SIZE=10000000
      
  postgres:
    image: postgres:14
    environment:
      - POSTGRES_DB=cdo
      - POSTGRES_PASSWORD=cdo
```

## 10. 验收标准

- [ ] 单机支持200并发用户
- [ ] 模型规模达到10000元素
- [ ] 查询响应P95 < 100ms
- [ ] 内存占用 < 1GB
- [ ] 零数据丢失