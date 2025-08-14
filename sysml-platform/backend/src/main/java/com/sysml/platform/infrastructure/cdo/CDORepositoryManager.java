package com.sysml.platform.infrastructure.cdo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistryPopulator;
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
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.acceptor.IAcceptor;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.postgresql.PostgreSQLAdapter;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.container.IPluginContainer;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** CDO Repository管理器 - 基于Spring Data CDO的正确实现 真正的CDO集成，写入PostgreSQL */
// @Component  // 暂时禁用，使用CDODirectRepository代替
@Slf4j
public class CDORepositoryManager {

  static {
    // 启用调试（可选）
    OMPlatform.INSTANCE.setDebugging(false);
    // OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);
    // OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);

    // 准备容器 - 这是关键！必须在静态块中初始化
    Net4jUtil.prepareContainer(IPluginContainer.INSTANCE);
    TCPUtil.prepareContainer(IPluginContainer.INSTANCE);
    CDONet4jUtil.prepareContainer(IPluginContainer.INSTANCE);
    CDONet4jServerUtil.prepareContainer(IPluginContainer.INSTANCE);
  }

  @Value("${cdo.server.port:2036}")
  private int serverPort;

  @Value("${cdo.repository.name:sysml}")
  private String repositoryName;

  private IManagedContainer container;
  private IRepository repository;
  private IAcceptor acceptor;
  private CDOSession session;
  private boolean initialized = false;

  @PostConstruct
  public void initialize() {
    try {
      log.info("Initializing CDO Repository Manager with PostgreSQL backend");

      // 1. 创建容器
      container = ContainerUtil.createContainer();
      Net4jUtil.prepareContainer(container);
      TCPUtil.prepareContainer(container);
      CDONet4jUtil.prepareContainer(container);
      CDONet4jServerUtil.prepareContainer(container);
      container.activate();

      // 2. 创建PostgreSQL存储
      IStore store = createPostgreSQLStore();

      // 3. 创建Repository
      Map<String, String> props = new HashMap<>();
      props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo-uuid");
      props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
      props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");

      repository = CDOServerUtil.createRepository(repositoryName, store, props);
      CDOServerUtil.addRepository(container, repository);

      // 4. 启动TCP服务器
      acceptor = Net4jUtil.getAcceptor(container, TCPUtil.FACTORY_TYPE, "0.0.0.0:" + serverPort);

      // 5. 创建本地Session（连接到自己的服务器）
      createLocalSession();

      initialized = true;
      log.info("CDO Repository Manager initialized successfully");
      log.info("CDO Server listening on port {}", serverPort);

    } catch (Exception e) {
      log.error("Failed to initialize CDO Repository Manager", e);
      initialized = false;
      throw new RuntimeException("CDO initialization failed", e);
    }
  }

  /** 创建PostgreSQL存储 */
  private IStore createPostgreSQLStore() {
    log.info("Creating PostgreSQL store for CDO");

    // PostgreSQL数据源配置
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setServerNames(new String[] {"localhost"});
    dataSource.setPortNumbers(new int[] {5432});
    dataSource.setDatabaseName("sysml_dev_db");
    dataSource.setUser("postgres");
    dataSource.setPassword("123456");

    // 创建映射策略
    IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);

    // 创建PostgreSQL适配器
    IDBAdapter dbAdapter = new PostgreSQLAdapter();

    // 创建DB存储
    IDBStore store =
        CDODBUtil.createStore(
            mappingStrategy, dbAdapter, DBUtil.createConnectionProvider(dataSource));

    log.info("PostgreSQL store created successfully");
    return store;
  }

  /** 创建本地Session */
  private void createLocalSession() {
    try {
      // 创建连接器（连接到本地服务器）
      IConnector connector =
          Net4jUtil.getConnector(container, TCPUtil.FACTORY_TYPE, "localhost:" + serverPort);

      // 创建Session配置
      CDONet4jSessionConfiguration config = CDONet4jUtil.createNet4jSessionConfiguration();
      config.setConnector(connector);
      config.setRepositoryName(repositoryName);
      config.setActivateOnOpen(true);

      // 打开Session
      session = config.openNet4jSession();

      // 填充包注册表
      CDOPackageRegistryPopulator.populate(session.getPackageRegistry());

      // 注册Ecore包
      session.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);

      // 设置Session选项
      session.options().setGeneratedPackageEmulationEnabled(true);
      session
          .options()
          .setLockNotificationMode(CDOCommonSession.Options.LockNotificationMode.ALWAYS);

      log.info("CDO Session created successfully");

    } catch (Exception e) {
      log.error("Failed to create CDO Session", e);
      throw new RuntimeException("CDO Session creation failed", e);
    }
  }

  /** 获取CDO Session */
  public CDOSession getSession() {
    if (!initialized || session == null || session.isClosed()) {
      throw new IllegalStateException("CDO Session not available");
    }
    return session;
  }

  /** 开启新事务 */
  public CDOTransaction openTransaction() {
    CDOSession currentSession = getSession();
    CDOTransaction transaction = currentSession.openTransaction();

    // 设置事务选项
    transaction.options().setAutoReleaseLocksEnabled(true);
    transaction.options().setLockNotificationEnabled(true);

    log.debug("CDO Transaction opened");
    return transaction;
  }

  /** 获取Repository名称 */
  public String getRepositoryName() {
    return repositoryName;
  }

  /** 健康检查 */
  public boolean isHealthy() {
    return initialized
        && repository != null
        && repository.getState() == IRepository.State.ONLINE
        && session != null
        && !session.isClosed();
  }

  /** 获取服务器信息 */
  public Map<String, Object> getServerInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put("initialized", initialized);
    info.put("repositoryName", repositoryName);
    info.put("serverPort", serverPort);

    if (repository != null) {
      info.put("repositoryUUID", repository.getUUID());
      info.put("repositoryState", repository.getState().toString());
    }

    if (session != null) {
      info.put("sessionID", session.getSessionID());
      info.put("sessionOpen", !session.isClosed());
    }

    return info;
  }

  @PreDestroy
  public void cleanup() {
    log.info("Shutting down CDO Repository Manager");

    if (session != null && !session.isClosed()) {
      session.close();
    }

    if (acceptor != null) {
      acceptor.close();
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
