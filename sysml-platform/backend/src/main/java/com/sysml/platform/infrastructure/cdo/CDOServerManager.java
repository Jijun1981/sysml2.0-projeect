package com.sysml.platform.infrastructure.cdo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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

/**
 * CDO服务器管理器 - 按照官方文档的标准实现
 * 真正的CDO + PostgreSQL集成，按照cdo-server.xml配置模式
 * 依赖CDOProtocolFactory确保协议工厂已注册
 */
@Component
@Slf4j  
public class CDOServerManager {
    
    private final CDOProtocolFactory protocolFactory;
    
    public CDOServerManager(CDOProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
        log.info("CDOServerManager构造 - 协议工厂依赖已注入");
    }
    
    static {
        // 1. 启用调试（可选）
        OMPlatform.INSTANCE.setDebugging(false);
        
        // 2. 准备所有容器 - 按照官方文档顺序
        try {
            Net4jUtil.prepareContainer(IPluginContainer.INSTANCE);
            TCPUtil.prepareContainer(IPluginContainer.INSTANCE);
            CDONet4jUtil.prepareContainer(IPluginContainer.INSTANCE);
            CDONet4jServerUtil.prepareContainer(IPluginContainer.INSTANCE);
            log.info("CDO容器静态初始化完成");
        } catch (Exception e) {
            log.error("CDO容器静态初始化失败", e);
        }
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
            log.info("初始化CDO服务器 - 标准PostgreSQL集成");
            
            // 1. 创建并激活容器
            container = ContainerUtil.createContainer();
            Net4jUtil.prepareContainer(container);
            TCPUtil.prepareContainer(container);
            CDONet4jUtil.prepareContainer(container);
            CDONet4jServerUtil.prepareContainer(container);
            container.activate();
            
            // 2. 确保基础包已注册
            EcorePackage.eINSTANCE.eClass();
            
            // 3. 创建PostgreSQL存储
            IStore store = createPostgreSQLStore();
            
            // 4. 创建Repository - 严格按照cdo-server.xml配置
            Map<String, String> props = new HashMap<>();
            props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo-uuid");
            props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
            props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");
            props.put(IRepository.Props.ENSURE_REFERENTIAL_INTEGRITY, "false");
            
            repository = CDOServerUtil.createRepository(repositoryName, store, props);
            CDOServerUtil.addRepository(container, repository);
            
            // 5. 启动TCP接受器 - 对应cdo-server.xml的acceptor
            acceptor = Net4jUtil.getAcceptor(container, TCPUtil.FACTORY_TYPE, "0.0.0.0:" + serverPort);
            
            // 等待服务器完全启动
            Thread.sleep(2000);
            
            // 6. 服务器模式：不创建客户端Session，仅提供Repository服务
            log.info("CDO服务器启动完成 - 仅服务器模式，不创建客户端Session");
            
            initialized = true;
            log.info("CDO服务器初始化成功");
            log.info("TCP接受器端口: {}", serverPort);
            log.info("Repository状态: {}", repository.getState());
            
        } catch (Exception e) {
            log.error("CDO服务器初始化失败", e);
            initialized = false;
            throw new RuntimeException("CDO服务器初始化失败", e);
        }
    }
    
    /**
     * 创建PostgreSQL存储 - 对应cdo-server.xml的store配置
     */
    private IStore createPostgreSQLStore() {
        log.info("创建PostgreSQL存储");
        
        try {
            // 对应cdo-server.xml中的dataSource配置
            PGSimpleDataSource dataSource = new PGSimpleDataSource();
            dataSource.setServerNames(new String[]{"localhost"});
            dataSource.setPortNumbers(new int[]{5432});
            dataSource.setDatabaseName("sysml_dev_db");
            dataSource.setUser("postgres");
            dataSource.setPassword("123456");
            
            // 对应cdo-server.xml中的mappingStrategy type="horizontal"
            IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);
            
            // 对应cdo-server.xml中的dbAdapter name="postgresql"
            IDBAdapter dbAdapter = new PostgreSQLAdapter();
            
            // 创建DBStore
            IDBStore store = CDODBUtil.createStore(
                mappingStrategy,
                dbAdapter,
                DBUtil.createConnectionProvider(dataSource)
            );
            
            log.info("PostgreSQL存储创建成功");
            return store;
            
        } catch (Exception e) {
            log.error("PostgreSQL存储创建失败", e);
            throw new RuntimeException("PostgreSQL存储创建失败", e);
        }
    }
    
    /**
     * 创建客户端Session - 连接到自己启动的服务器
     */
    private void createClientSession() {
        try {
            log.info("创建CDO客户端Session");
            
            // 确保容器中也有协议工厂
            CDONet4jServerUtil.prepareContainer(container);
            
            // 连接到本地服务器
            IConnector connector = Net4jUtil.getConnector(container, TCPUtil.FACTORY_TYPE, "localhost:" + serverPort);
            
            // 配置Session
            CDONet4jSessionConfiguration config = CDONet4jUtil.createNet4jSessionConfiguration();
            config.setConnector(connector);
            config.setRepositoryName(repositoryName);
            config.setActivateOnOpen(true);
            
            // 打开Session
            session = config.openNet4jSession();
            
            // 关键：填充包注册表
            CDOPackageRegistryPopulator.populate(session.getPackageRegistry());
            session.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);
            
            // 设置Session选项
            session.options().setGeneratedPackageEmulationEnabled(true);
            
            log.info("CDO客户端Session创建成功");
            
        } catch (Exception e) {
            log.error("CDO客户端Session创建失败", e);
            throw new RuntimeException("CDO客户端Session创建失败", e);
        }
    }
    
    /**
     * 获取CDO Repository - 服务器模式
     */
    public IRepository getRepository() {
        if (!initialized || repository == null) {
            throw new IllegalStateException("CDO Repository不可用");
        }
        return repository;
    }
    
    /**
     * 创建外部客户端Session工厂方法
     * 外部客户端需要连接到此服务器时使用
     */
    public CDOSession createExternalSession() {
        if (!initialized) {
            throw new IllegalStateException("CDO服务器未初始化");
        }
        
        try {
            // 为外部客户端创建连接器
            IConnector connector = Net4jUtil.getConnector(container, TCPUtil.FACTORY_TYPE, "localhost:" + serverPort);
            
            // 配置Session
            CDONet4jSessionConfiguration config = CDONet4jUtil.createNet4jSessionConfiguration();
            config.setConnector(connector);
            config.setRepositoryName(repositoryName);
            config.setActivateOnOpen(true);
            
            // 创建并返回Session
            CDOSession externalSession = config.openNet4jSession();
            
            // 配置Session
            CDOPackageRegistryPopulator.populate(externalSession.getPackageRegistry());
            externalSession.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);
            externalSession.options().setGeneratedPackageEmulationEnabled(true);
            
            return externalSession;
            
        } catch (Exception e) {
            log.error("创建外部客户端Session失败", e);
            throw new RuntimeException("创建外部客户端Session失败", e);
        }
    }
    
    /**
     * 便利方法：为外部客户端开启事务
     */
    public CDOTransaction openExternalTransaction() {
        CDOSession externalSession = createExternalSession();
        CDOTransaction transaction = externalSession.openTransaction();
        transaction.options().setAutoReleaseLocksEnabled(true);
        return transaction;
    }
    
    /**
     * 获取Repository名称
     */
    public String getRepositoryName() {
        return repositoryName;
    }
    
    /**
     * 健康检查 - 服务器模式
     */
    public boolean isHealthy() {
        return initialized && 
               repository != null && 
               repository.getState() == IRepository.State.ONLINE &&
               acceptor != null;
    }
    
    /**
     * 获取服务器信息
     */
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("initialized", initialized);
        info.put("repositoryName", repositoryName);
        info.put("serverPort", serverPort);
        info.put("mode", "STANDARD_CDO_SERVER");
        
        if (repository != null) {
            info.put("repositoryUUID", repository.getUUID());
            info.put("repositoryState", repository.getState().toString());
            info.put("storeType", "PostgreSQL");
        }
        
        if (acceptor != null) {
            info.put("acceptorPresent", true);
            info.put("acceptorPort", serverPort);
        }
        
        return info;
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("关闭CDO服务器");
        
        // 服务器模式：不需要关闭session
        
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