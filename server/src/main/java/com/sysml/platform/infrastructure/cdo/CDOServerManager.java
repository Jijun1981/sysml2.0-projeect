package com.sysml.platform.infrastructure.cdo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.net4j.CDONet4jServerUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.postgresql.PostgreSQLAdapter;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.jvm.JVMUtil;
import org.eclipse.emf.cdo.server.embedded.CDOEmbeddedRepositoryConfig;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/** CDO服务器管理器 - 管理CDO Repository和Session 真正的CDO集成实现 */
@Component
@ConditionalOnProperty(
    prefix = "cdo",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@Slf4j
public class CDOServerManager {

  @Value("${cdo.server.port:2037}")
  private int serverPort;

  @Value("${cdo.repository.name:sysml}")
  private String repositoryName;

  @Value("${cdo.store.schema-name:sysml}")
  private String schemaName;

  @Value("${cdo.store.drop-on-activate:false}")
  private boolean dropOnActivate;

  @Value("${spring.datasource.url:}")
  private String datasourceUrl;

  @Autowired(required = false)
  private javax.sql.DataSource dataSource;

  private IManagedContainer container;
  private IRepository repository;
  private CDOSession session;
  private boolean initialized = false;

  @PostConstruct
  public void initialize() {
    try {
      log.info("Initializing CDO Server on port {}", serverPort);

      // 1. 创建并激活容器
      container = ContainerUtil.createContainer();
      Net4jUtil.prepareContainer(container);
      TCPUtil.prepareContainer(container);
      JVMUtil.prepareContainer(container);
      CDONet4jUtil.prepareContainer(container);
      CDONet4jServerUtil.prepareContainer(container);
      container.activate();
      log.info("Container activated successfully");

      // 2. 启动acceptors（必须在Repository添加到Container之前）
      var jvmAcceptor = JVMUtil.getAcceptor(container, "default");
      var tcpAcceptor = Net4jUtil.getAcceptor(container, TCPUtil.FACTORY_TYPE, "0.0.0.0:" + serverPort);
      log.info("Acceptors started: JVM and TCP on port {}", serverPort);

      // 3. 创建并配置Store
      IStore store = createDatabaseStore();
      log.info("Database store created");

      // 4. 创建Repository配置
      Map<String, String> props = new HashMap<>();
      props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo-uuid");
      props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
      props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");
      
      // 关键配置: 确保Repository能正确初始化
      props.put(IRepository.Props.ENSURE_REFERENTIAL_INTEGRITY, "false");  // 初始化时关闭引用完整性检查
      props.put(IRepository.Props.SERIALIZE_COMMITS, "false");  // 初始化时关闭提交序列化

      repository = CDOServerUtil.createRepository(repositoryName, store, props);
      log.info("Repository created with name: {}", repositoryName);
      
      // 5. 关键步骤：激活Repository（通过添加到Container）
      CDOServerUtil.addRepository(container, repository);
      log.info("Repository added to container");

      // 6. 等待Repository完全激活 - 增加重试次数和延迟
      waitForRepositoryActivation();
      
      // 7. 如果Repository仍未ONLINE，尝试强制激活
      if (repository.getState() != IRepository.State.ONLINE) {
        log.warn("Repository not ONLINE after initial activation, attempting forced activation");
        performForcedActivation();
      }

      // 8. 创建初始Session并注册必要的EPackage
      initializeSessionAndPackages();

      // 9. 最终验证
      if (repository.getState() == IRepository.State.ONLINE && session != null && !session.isClosed()) {
        initialized = true;
        log.info("✅ CDO Server initialized successfully - Repository ONLINE");
      } else {
        throw new RuntimeException("CDO Repository failed to reach ONLINE state");
      }

    } catch (Exception e) {
      log.error("❌ Failed to initialize CDO Server", e);
      initialized = false;
      // 清理部分初始化的资源
      cleanup();
    }
  }

  /**
   * 等待Repository激活到ONLINE状态
   */
  private void waitForRepositoryActivation() throws InterruptedException {
    int maxRetries = 30;  // 增加到30次重试
    int retryDelayMs = 200;  // 增加延迟到200ms
    
    for (int i = 0; i < maxRetries; i++) {
      if (repository.getState() == IRepository.State.ONLINE) {
        log.info("✅ Repository reached ONLINE state after {} attempts", i + 1);
        return;
      }
      
      log.debug("Repository state: {} (attempt {}/{})", repository.getState(), i + 1, maxRetries);
      Thread.sleep(retryDelayMs);
    }
    
    log.warn("⚠️ Repository still in state {} after {} retries", repository.getState(), maxRetries);
  }

  /**
   * 强制激活Repository
   */
  private void performForcedActivation() {
    try {
      // 方法1: 直接激活Repository（如果是InternalRepository）
      if (repository instanceof InternalRepository) {
        InternalRepository internalRepo = (InternalRepository) repository;
        if (!internalRepo.isActive()) {
          log.info("Attempting to directly activate Repository");
          LifecycleUtil.activate(internalRepo);
          Thread.sleep(500);
          log.info("Direct activation completed, state: {}", repository.getState());
        }
      }
      
      // 方法2: 创建初始连接来触发激活
      if (repository.getState() != IRepository.State.ONLINE) {
        log.info("Attempting activation via initial connection");
        createInitialConnection();
      }
      
    } catch (Exception e) {
      log.error("Failed forced activation", e);
    }
  }

  /**
   * 创建初始连接以触发Repository激活
   */
  private void createInitialConnection() throws Exception {
    try {
      // 创建初始Session
      String description = "jvm://localhost/" + repositoryName;
      var connector = JVMUtil.getConnector(container, description);
      CDONet4jSessionConfiguration cfg = CDONet4jUtil.createNet4jSessionConfiguration();
      cfg.setConnector(connector);
      cfg.setRepositoryName(repositoryName);
      
      // 设置Session配置以帮助激活
      cfg.setPassiveUpdateEnabled(false);  // 禁用被动更新以简化初始化
      // cfg.setRevisionPrefetchingPolicy(CDOUtil.createRevisionPrefetchingPolicy(0));  // 方法在此CDO版本中不存在
      
      CDOSession tempSession = cfg.openSession();
      log.info("Initial connection established");
      
      // 创建并立即提交一个事务（这通常会触发Repository完全激活）
      CDOTransaction transaction = tempSession.openTransaction();
      
      // 注册Ecore包到Session
      tempSession.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);
      log.info("EcorePackage registered to initial session");
      
      // 提交空事务
      transaction.commit();
      transaction.close();
      
      // 等待激活传播
      Thread.sleep(1000);
      
      // 检查Repository状态
      if (repository.getState() == IRepository.State.ONLINE) {
        log.info("✅ Repository activated successfully via initial connection");
        this.session = tempSession;  // 保存这个Session
      } else {
        log.warn("Repository still not ONLINE after initial connection: {}", repository.getState());
        tempSession.close();
      }
      
    } catch (Exception e) {
      log.error("Failed to create initial connection", e);
      throw e;
    }
  }

  /**
   * 初始化Session并注册必要的EPackage
   */
  private void initializeSessionAndPackages() throws Exception {
    if (session == null || session.isClosed()) {
      createEmbeddedSession();
    }
    
    if (session != null && !session.isClosed()) {
      // 注册核心EPackage
      session.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);
      log.info("EcorePackage registered to main session");
      
      // 注册项目特定的SysML包（如果存在）
      registerProjectPackages();
    } else {
      throw new RuntimeException("Failed to create CDO Session");
    }
  }

  /**
   * 注册项目特定的EPackage
   */
  private void registerProjectPackages() {
    try {
      // 注册SysML相关的包
      String[] sysmlPackageUris = {
        "https://www.omg.org/spec/SysML/20250201",
        "https://www.omg.org/spec/KerML/20250201",
        "http://www.omg.org/2014/Types"
      };
      
      for (String uri : sysmlPackageUris) {
        EPackage pkg = EPackage.Registry.INSTANCE.getEPackage(uri);
        if (pkg != null) {
          session.getPackageRegistry().putEPackage(pkg);
          log.info("Registered project package: {}", uri);
        } else {
          log.debug("Package not found in global registry: {}", uri);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to register some project packages", e);
      // 不抛出异常，因为这不是致命错误
    }
  }

  /** 创建数据库存储 */
  private IStore createDatabaseStore() {
    try {
      log.info(
          "Creating DB store for CDO using injected DataSource: {}",
          dataSource != null ? dataSource.getClass().getName() : "null");
      
      // 预先确保schema存在
      ensureSchemaExists();
      
      // 创建映射策略
      IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);

      // 选择适配器：PostgreSQL Only
      IDBAdapter dbAdapter = new PostgreSQLAdapter();

      // 创建DB存储配置（避免重复创建schema）
      java.util.Map<String, String> storeProps = new java.util.HashMap<>();
      storeProps.put(IDBStore.Props.SCHEMA_NAME, schemaName);
      storeProps.put(IDBStore.Props.PREPEND_SCHEMA_NAME, "true");  // 使用schema前缀
      storeProps.put(IDBStore.Props.CREATE_SCHEMA_IF_NEEDED, "false");  // 不让CDO创建schema，我们已经创建了
      storeProps.put(IDBStore.Props.DROP_ALL_DATA_ON_ACTIVATE, Boolean.toString(dropOnActivate));
      IDBStore store =
          CDODBUtil.createStore(
              mappingStrategy, dbAdapter, DBUtil.createConnectionProvider(dataSource), storeProps);

      // Store会在Repository激活时自动激活

      log.info(
          "CDO DB store created successfully with adapter: {}",
          dbAdapter.getClass().getSimpleName());
      return store;

    } catch (Exception e) {
      log.error("Failed to create DB store for CDO", e);
      throw new RuntimeException("CDO DB store initialization failed", e);
    }
  }

  // PostgreSQL Only: no dynamic adapter selection

  /** 使用嵌入式模式创建Session */
  private void createEmbeddedSession() {
    try {
      // 验证Repository状态
      if (repository == null) {
        throw new IllegalStateException("Repository is null");
      }
      
      if (repository.getState() != IRepository.State.ONLINE) {
        log.warn("Repository is not ONLINE ({}), attempting session creation anyway", repository.getState());
      }
      
      // 尝试使用JVM内部连接（嵌入式模式）
      String description = "jvm://localhost/" + repositoryName;
      var connector = JVMUtil.getConnector(container, description);
      CDONet4jSessionConfiguration cfg = CDONet4jUtil.createNet4jSessionConfiguration();
      cfg.setConnector(connector);
      cfg.setRepositoryName(repositoryName);
      
      // 配置Session以提高稳定性
      cfg.setPassiveUpdateEnabled(false);
      // cfg.setRevisionPrefetchingPolicy(CDOUtil.createRevisionPrefetchingPolicy(0));  // 方法在此CDO版本中不存在
      
      this.session = cfg.openSession();
      log.info("✅ CDO session opened to repo '{}' via JVM connector", repositoryName);
      
      // 验证Session状态
      if (session.isClosed()) {
        throw new RuntimeException("Session was closed immediately after creation");
      }
      
    } catch (Exception e) {
      log.warn("❌ Failed to create JVM session: {}, trying TCP", e.getMessage());
      try {
        // 回退到TCP连接
        var connector = TCPUtil.getConnector(container, "localhost:" + serverPort);
        CDONet4jSessionConfiguration cfg = CDONet4jUtil.createNet4jSessionConfiguration();
        cfg.setConnector(connector);
        cfg.setRepositoryName(repositoryName);
        
        // 配置Session
        cfg.setPassiveUpdateEnabled(false);
        // cfg.setRevisionPrefetchingPolicy(CDOUtil.createRevisionPrefetchingPolicy(0));  // 方法在此CDO版本中不存在
        
        this.session = cfg.openSession();
        log.info("✅ CDO session opened to repo '{}' via TCP", repositoryName);
        
        // 验证Session状态
        if (session.isClosed()) {
          throw new RuntimeException("TCP Session was closed immediately after creation");
        }
        
      } catch (Exception tcpError) {
        log.error("❌ Failed to create CDO session via both JVM and TCP. JVM error: {}, TCP error: {}", 
                 e.getMessage(), tcpError.getMessage());
        this.session = null;
        // 不抛出异常，让调用方处理
      }
    }
  }

  /** 获取CDO Session */
  public CDOSession getSession() {
    if (!initialized || session == null || (session != null && session.isClosed())) {
      createEmbeddedSession();
    }
    return session;
  }

  /** 开始新事务 */
  public CDOTransaction openTransaction() {
    if (session == null || session.isClosed()) {
      createEmbeddedSession();
    }
    return session.openTransaction();
  }

  /** 获取Repository用于嵌入式访问 */
  public IRepository getRepository() {
    return repository;
  }

  /** 健康检查 */
  public boolean isHealthy() {
    return initialized && repository != null && repository.getState() == IRepository.State.ONLINE;
  }

  /** 获取服务器信息 */
  public Map<String, Object> getServerInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put("initialized", initialized);
    info.put("port", serverPort);
    info.put("repositoryName", repositoryName);

    if (repository != null) {
      info.put("repositoryUUID", repository.getUUID());
      info.put("repositoryState", repository.getState());
      
      // 添加Repository详细状态信息
      if (repository instanceof InternalRepository) {
        InternalRepository internalRepo = (InternalRepository) repository;
        info.put("repositoryActive", internalRepo.isActive());
        
        // PackageRegistry状态
        var packageRegistry = internalRepo.getPackageRegistry();
        if (packageRegistry != null) {
          info.put("packageRegistryAvailable", true);
          info.put("registeredPackageCount", packageRegistry.size());
        } else {
          info.put("packageRegistryAvailable", false);
        }
      }
    } else {
      info.put("repositoryState", "NULL");
    }

    if (session != null) {
      info.put("sessionID", session.getSessionID());
      info.put("sessionOpen", !session.isClosed());
      
      // Session PackageRegistry状态
      var sessionPackageRegistry = session.getPackageRegistry();
      if (sessionPackageRegistry != null) {
        info.put("sessionPackageRegistryAvailable", true);
        info.put("sessionRegisteredPackageCount", sessionPackageRegistry.size());
      } else {
        info.put("sessionPackageRegistryAvailable", false);
      }
    } else {
      info.put("sessionOpen", false);
    }

    // Container状态
    if (container != null) {
      info.put("containerActive", container.isActive());
    }

    return info;
  }

  /**
   * 诊断CDO状态 - 用于调试
   */
  public Map<String, Object> getDiagnosticInfo() {
    Map<String, Object> diagnostic = new HashMap<>();
    
    try {
      // 基本状态
      diagnostic.put("timestamp", System.currentTimeMillis());
      diagnostic.put("initialized", initialized);
      
      // Container诊断
      if (container != null) {
        diagnostic.put("container.active", container.isActive());
        diagnostic.put("container.class", container.getClass().getSimpleName());
      } else {
        diagnostic.put("container.status", "NULL");
      }
      
      // Repository诊断
      if (repository != null) {
        diagnostic.put("repository.state", repository.getState().toString());
        diagnostic.put("repository.uuid", repository.getUUID());
        diagnostic.put("repository.name", repository.getName());
        diagnostic.put("repository.class", repository.getClass().getSimpleName());
        
        if (repository instanceof InternalRepository) {
          InternalRepository internalRepo = (InternalRepository) repository;
          diagnostic.put("repository.active", internalRepo.isActive());
          
          // Store状态
          var store = internalRepo.getStore();
          if (store != null) {
            diagnostic.put("store.active", store.isActive());
            diagnostic.put("store.class", store.getClass().getSimpleName());
          } else {
            diagnostic.put("store.status", "NULL");
          }
          
          // PackageRegistry诊断
          var packageRegistry = internalRepo.getPackageRegistry();
          if (packageRegistry != null) {
            diagnostic.put("packageRegistry.available", true);
            diagnostic.put("packageRegistry.size", packageRegistry.size());
            diagnostic.put("packageRegistry.class", packageRegistry.getClass().getSimpleName());
          } else {
            diagnostic.put("packageRegistry.available", false);
          }
        }
      } else {
        diagnostic.put("repository.status", "NULL");
      }
      
      // Session诊断
      if (session != null) {
        diagnostic.put("session.id", session.getSessionID());
        diagnostic.put("session.closed", session.isClosed());
        diagnostic.put("session.class", session.getClass().getSimpleName());
        
        var sessionPackageRegistry = session.getPackageRegistry();
        if (sessionPackageRegistry != null) {
          diagnostic.put("session.packageRegistry.available", true);
          diagnostic.put("session.packageRegistry.size", sessionPackageRegistry.size());
        } else {
          diagnostic.put("session.packageRegistry.available", false);
        }
      } else {
        diagnostic.put("session.status", "NULL");
      }
      
    } catch (Exception e) {
      diagnostic.put("diagnostic.error", e.getMessage());
      log.error("Error during diagnostic", e);
    }
    
    return diagnostic;
  }

  private void ensureSchemaExists() {
    if (dataSource == null) {
      log.warn("DataSource is null, skipping schema check");
      return;
    }
    
    try (var connection = dataSource.getConnection()) {
      var dbMetaData = connection.getMetaData();
      var schemas = dbMetaData.getSchemas();
      boolean schemaExists = false;
      
      while (schemas.next()) {
        String existingSchema = schemas.getString("TABLE_SCHEM");
        if (schemaName.equalsIgnoreCase(existingSchema)) {
          schemaExists = true;
          break;
        }
      }
      
      if (!schemaExists) {
        log.info("Schema '{}' does not exist, creating it", schemaName);
        try (var stmt = connection.createStatement()) {
          stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
          log.info("Schema '{}' created successfully", schemaName);
        }
      } else {
        log.info("Schema '{}' already exists", schemaName);
        
        // 检查是否需要清理现有表
        if (dropOnActivate) {
          log.info("dropOnActivate=true, cleaning existing CDO tables in schema '{}'", schemaName);
          cleanExistingCDOTables(connection);
        }
      }
    } catch (Exception e) {
      log.error("Failed to ensure schema exists", e);
      // 不抛出异常，让CDO自己处理
    }
  }
  
  private void cleanExistingCDOTables(java.sql.Connection connection) {
    try {
      // 获取schema中的所有表
      var dbMetaData = connection.getMetaData();
      var tables = dbMetaData.getTables(null, schemaName, "%", new String[]{"TABLE"});
      
      var tableNames = new java.util.ArrayList<String>();
      while (tables.next()) {
        String tableName = tables.getString("TABLE_NAME");
        // CDO表包括cdo_开头的表和其他CDO相关表
        if (tableName.toLowerCase().startsWith("cdo_") || 
            tableName.toLowerCase().contains("cdo") ||
            tableName.toLowerCase().startsWith("list_") ||
            tableName.toLowerCase().startsWith("external_")) {
          tableNames.add(tableName);
        }
      }
      
      if (!tableNames.isEmpty()) {
        log.info("Found {} CDO tables to clean: {}", tableNames.size(), tableNames);
        
        try (var stmt = connection.createStatement()) {
          // 禁用外键约束
          stmt.execute("SET session_replication_role = 'replica'");
          
          // 删除所有CDO表
          for (String tableName : tableNames) {
            String dropSql = String.format("DROP TABLE IF EXISTS %s.%s CASCADE", schemaName, tableName);
            log.debug("Executing: {}", dropSql);
            stmt.execute(dropSql);
          }
          
          // 重新启用外键约束
          stmt.execute("SET session_replication_role = 'origin'");
          
          log.info("Successfully cleaned {} CDO tables", tableNames.size());
        }
      } else {
        log.info("No existing CDO tables found in schema '{}'", schemaName);
      }
    } catch (Exception e) {
      log.error("Failed to clean existing CDO tables", e);
      // 不抛出异常，继续初始化
    }
  }

  @PreDestroy
  public void cleanup() {
    log.info("Shutting down CDO Server");

    if (session != null && !session.isClosed()) {
      session.close();
    }

    if (repository != null) {
      LifecycleUtil.deactivate(repository);
    }

    if (container != null) {
      container.deactivate();
    }

    initialized = false;
  }
}
