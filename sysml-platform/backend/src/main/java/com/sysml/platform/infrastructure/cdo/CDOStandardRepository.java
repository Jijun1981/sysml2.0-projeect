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
import org.eclipse.emf.ecore.EObject;
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
 * CDO标准Repository管理器 - 按照官方文档的完整实现
 * 真正的CDO + PostgreSQL集成，无简化
 */
// @Component  // 禁用，使用CDOServerManager代替
@Slf4j
public class CDOStandardRepository {
    
    static {
        // 启用调试
        OMPlatform.INSTANCE.setDebugging(false);
        
        // 准备所有必要的容器 - 这是关键！
        Net4jUtil.prepareContainer(IPluginContainer.INSTANCE);
        TCPUtil.prepareContainer(IPluginContainer.INSTANCE);
        CDONet4jUtil.prepareContainer(IPluginContainer.INSTANCE);
        CDONet4jServerUtil.prepareContainer(IPluginContainer.INSTANCE);
        
        log.info("CDO容器准备完毕");
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
            log.info("初始化CDO标准Repository - 完整PostgreSQL集成");
            
            // 1. 创建托管容器
            container = ContainerUtil.createContainer();
            Net4jUtil.prepareContainer(container);
            TCPUtil.prepareContainer(container);
            CDONet4jUtil.prepareContainer(container);
            CDONet4jServerUtil.prepareContainer(container);
            container.activate();
            
            // 2. 确保Ecore包被正确注册
            EcorePackage.eINSTANCE.eClass(); 
            
            // 3. 创建PostgreSQL存储
            IStore store = createPostgreSQLStore();
            
            // 4. 创建Repository - 按照官方文档配置
            Map<String, String> props = new HashMap<>();
            props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo-uuid");
            props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
            props.put(IRepository.Props.SUPPORTING_BRANCHES, "false"); 
            props.put(IRepository.Props.ENSURE_REFERENTIAL_INTEGRITY, "false");
            
            repository = CDOServerUtil.createRepository(repositoryName, store, props);
            CDOServerUtil.addRepository(container, repository);
            
            // 5. 启动TCP接受器 - 标准CDO服务器方式
            acceptor = Net4jUtil.getAcceptor(container, TCPUtil.FACTORY_TYPE, "0.0.0.0:" + serverPort);
            
            // 6. 创建客户端Session连接到自己的服务器
            createClientSession();
            
            initialized = true;
            log.info("CDO标准Repository初始化成功");
            log.info("CDO服务器监听端口: {}", serverPort);
            log.info("Repository状态: {}", repository.getState());
            
        } catch (Exception e) {
            log.error("CDO标准Repository初始化失败", e);
            initialized = false;
            throw new RuntimeException("CDO初始化失败", e);
        }
    }
    
    /**
     * 创建PostgreSQL存储 - 按照官方DBStore配置
     */
    private IStore createPostgreSQLStore() {
        log.info("创建PostgreSQL DBStore");
        
        try {
            // PostgreSQL数据源 - 对应cdo-server.xml中的配置
            PGSimpleDataSource dataSource = new PGSimpleDataSource();
            dataSource.setServerNames(new String[]{"localhost"});
            dataSource.setPortNumbers(new int[]{5432});
            dataSource.setDatabaseName("sysml_dev_db");
            dataSource.setUser("postgres");
            dataSource.setPassword("123456");
            
            // 水平映射策略 - 标准推荐
            IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);
            
            // PostgreSQL适配器
            IDBAdapter dbAdapter = new PostgreSQLAdapter();
            
            // 创建DBStore
            IDBStore store = CDODBUtil.createStore(
                mappingStrategy, 
                dbAdapter, 
                DBUtil.createConnectionProvider(dataSource)
            );
            
            log.info("PostgreSQL DBStore创建成功");
            return store;
            
        } catch (Exception e) {
            log.error("PostgreSQL DBStore创建失败", e);
            throw new RuntimeException("PostgreSQL存储创建失败", e);
        }
    }
    
    /**
     * 创建客户端Session - 标准CDO客户端连接方式
     */
    private void createClientSession() {
        try {
            log.info("创建CDO客户端Session");
            
            // 创建连接器连接到本地服务器
            IConnector connector = Net4jUtil.getConnector(container, TCPUtil.FACTORY_TYPE, "localhost:" + serverPort);
            
            // 创建Session配置
            CDONet4jSessionConfiguration config = CDONet4jUtil.createNet4jSessionConfiguration();
            config.setConnector(connector);
            config.setRepositoryName(repositoryName);
            config.setActivateOnOpen(true);
            
            // 打开Session
            session = config.openNet4jSession();
            
            // 填充包注册表 - 这是关键！
            CDOPackageRegistryPopulator.populate(session.getPackageRegistry());
            
            // 注册Ecore包
            session.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);
            
            // 设置Session选项
            session.options().setGeneratedPackageEmulationEnabled(true);
            
            log.info("CDO客户端Session创建成功");
            
        } catch (Exception e) {
            log.error("CDO客户端Session创建失败", e);
            throw new RuntimeException("CDO Session创建失败", e);
        }
    }
    
    /**
     * 保存EObject到CDO Repository - 标准CDO事务方式
     */
    public String saveModel(EObject model, String resourcePath) {
        if (!initialized || session == null || session.isClosed()) {
            throw new IllegalStateException("CDO Session不可用");
        }
        
        if (model == null) {
            throw new IllegalArgumentException("模型不能为空");
        }
        
        CDOTransaction transaction = null;
        try {
            // 开启CDO事务
            transaction = session.openTransaction();
            
            // 确保路径格式正确
            String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
            
            // 获取或创建CDO资源
            org.eclipse.emf.cdo.eresource.CDOResource resource = transaction.getOrCreateResource(path);
            
            // 添加模型到资源
            resource.getContents().clear();
            resource.getContents().add(model);
            
            // 提交事务到PostgreSQL
            transaction.commit();
            
            log.info("模型成功保存到CDO/PostgreSQL: {}", path);
            return path;
            
        } catch (Exception e) {
            log.error("CDO模型保存失败", e);
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (Exception ex) {
                    log.error("事务回滚失败", ex);
                }
            }
            throw new RuntimeException("CDO保存失败: " + e.getMessage(), e);
        } finally {
            if (transaction != null && !transaction.isClosed()) {
                transaction.close();
            }
        }
    }
    
    /**
     * 开启新事务 - 标准CDO方式
     */
    public CDOTransaction openTransaction() {
        if (!initialized || session == null || session.isClosed()) {
            throw new IllegalStateException("CDO Session不可用");
        }
        
        CDOTransaction transaction = session.openTransaction();
        transaction.options().setAutoReleaseLocksEnabled(true);
        
        log.debug("CDO事务已打开");
        return transaction;
    }
    
    /**
     * 获取Repository名称
     */
    public String getRepositoryName() {
        return repositoryName;
    }
    
    /**
     * 健康检查
     */
    public boolean isHealthy() {
        return initialized && 
               repository != null && 
               repository.getState() == IRepository.State.ONLINE &&
               session != null && 
               !session.isClosed();
    }
    
    /**
     * 获取服务器信息
     */
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("initialized", initialized);
        info.put("repositoryName", repositoryName);
        info.put("serverPort", serverPort);
        info.put("mode", "STANDARD_CDO");
        
        if (repository != null) {
            info.put("repositoryUUID", repository.getUUID());
            info.put("repositoryState", repository.getState().toString());
            info.put("storeType", "PostgreSQL_DBStore");
        }
        
        if (session != null) {
            info.put("sessionID", session.getSessionID());
            info.put("sessionOpen", !session.isClosed());
        }
        
        return info;
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("关闭CDO标准Repository");
        
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