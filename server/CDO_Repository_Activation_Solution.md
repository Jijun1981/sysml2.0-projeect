# CDO Repository激活问题解决方案

## 问题总结

在SysML2平台中遇到的CDO Repository激活问题包括：

1. **Repository状态停留在INITIAL，无法转为ONLINE**
2. **Repository的PackageRegistry为null，导致无法注册EPackage**
3. **连接时报错"Repository not found: sysml-test"**

## 根本原因分析

通过深入研究CDO官方文档和源码，发现了以下关键问题：

### 1. 激活顺序错误
- CDO Repository需要特定的初始化顺序
- Container必须在Repository之前激活
- Acceptors必须在Repository添加到Container之前启动

### 2. PackageRegistry初始化时机
- PackageRegistry只有在Repository完全激活后才可用
- 需要至少一个活跃的Session才能触发Repository完全激活
- CDO需要包含对象实例的事务来自动注册EPackage

### 3. Store激活依赖
- Repository状态依赖于底层Store的激活状态
- 数据库连接和Schema初始化必须在Store激活前完成

## 解决方案

### 核心修改

在`CDOServerManager.java`中实现了以下关键改进：

#### 1. 正确的初始化顺序
```java
// 1. 创建并激活容器
container = ContainerUtil.createContainer();
// ... 准备容器组件
container.activate();

// 2. 启动acceptors（必须在Repository添加前）
var jvmAcceptor = JVMUtil.getAcceptor(container, "default");
var tcpAcceptor = Net4jUtil.getAcceptor(container, TCPUtil.FACTORY_TYPE, "0.0.0.0:" + serverPort);

// 3. 创建Store
IStore store = createDatabaseStore();

// 4. 创建Repository
repository = CDOServerUtil.createRepository(repositoryName, store, props);

// 5. 添加到Container（触发激活）
CDOServerUtil.addRepository(container, repository);
```

#### 2. 强制激活机制
```java
private void performForcedActivation() {
    // 方法1: 直接激活Repository
    if (repository instanceof InternalRepository) {
        InternalRepository internalRepo = (InternalRepository) repository;
        if (!internalRepo.isActive()) {
            LifecycleUtil.activate(internalRepo);
        }
    }
    
    // 方法2: 创建初始连接触发激活
    if (repository.getState() != IRepository.State.ONLINE) {
        createInitialConnection();
    }
}
```

#### 3. 初始连接激活
```java
private void createInitialConnection() throws Exception {
    // 创建初始Session
    CDOSession tempSession = cfg.openSession();
    
    // 创建事务并提交（触发Repository完全激活）
    CDOTransaction transaction = tempSession.openTransaction();
    tempSession.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);
    transaction.commit();
    transaction.close();
}
```

#### 4. 增强的配置参数
```java
// 关键配置: 确保Repository能正确初始化
props.put(IRepository.Props.ENSURE_REFERENTIAL_INTEGRITY, "false");
props.put(IRepository.Props.SERIALIZE_COMMITS, "false");
```

#### 5. 改进的等待机制
```java
private void waitForRepositoryActivation() throws InterruptedException {
    int maxRetries = 30;  // 增加到30次重试
    int retryDelayMs = 200;  // 增加延迟到200ms
    
    for (int i = 0; i < maxRetries; i++) {
        if (repository.getState() == IRepository.State.ONLINE) {
            return;
        }
        Thread.sleep(retryDelayMs);
    }
}
```

### 诊断功能

添加了全面的诊断功能：

#### 1. 详细状态监控
```java
public Map<String, Object> getDiagnosticInfo() {
    // Container、Repository、Store、Session、PackageRegistry状态
}
```

#### 2. 测试端点
- `/api/test/cdo/diagnostic` - 详细诊断信息
- `/api/test/cdo/test-repository` - Repository连接测试
- `/api/test/cdo/integration-status` - 集成状态检查

## 关键发现

### CDO Repository激活机制

1. **Repository状态转换**：INITIAL → ONLINE
   - 需要底层Store完全激活
   - 需要至少一个Session连接
   - 需要至少一个成功的事务

2. **PackageRegistry初始化**：
   - Repository级别：在Repository激活时初始化
   - Session级别：在Session创建时初始化
   - 自动注册：需要包含对象实例的事务

3. **激活触发条件**：
   - `CDOServerUtil.addRepository(container, repository)` 触发激活
   - 首次Session连接可能触发完全激活
   - 成功的事务提交确保状态转换

### 常见错误原因

1. **"Repository not found"**：
   - Repository未正确添加到Container
   - Acceptor未启动或配置错误
   - Repository名称不匹配

2. **PackageRegistry为null**：
   - Repository未完全激活
   - Session未正确创建
   - 访问时机过早

3. **状态停留在INITIAL**：
   - Store未激活
   - 数据库连接问题
   - 配置参数错误

## 测试验证

使用以下端点验证修复效果：

```bash
# 检查Repository状态
curl http://localhost:8080/api/test/cdo/diagnostic

# 测试Repository连接
curl http://localhost:8080/api/test/cdo/test-repository

# 验证集成状态
curl http://localhost:8080/api/test/cdo/integration-status
```

## 最佳实践

### 1. 初始化顺序
1. 创建Container并激活
2. 启动Acceptors
3. 创建Store
4. 创建Repository
5. 添加Repository到Container
6. 等待激活完成
7. 创建Session
8. 注册EPackages

### 2. 错误处理
- 增加重试机制
- 实现降级策略
- 提供详细诊断信息
- 监控状态转换

### 3. 配置优化
- 初始化时关闭不必要的功能
- 优化数据库连接参数
- 使用适当的超时设置

## 结论

通过实现正确的初始化顺序、强制激活机制和全面的诊断功能，成功解决了CDO Repository激活问题。关键在于理解CDO的内部激活机制，并确保所有组件按正确顺序初始化。

修改后的代码现在能够：
- 可靠地将Repository从INITIAL状态转换为ONLINE
- 正确初始化PackageRegistry
- 成功创建和维护Session连接
- 提供详细的诊断和监控能力