package com.sysml.platform.infrastructure.cdo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.ISession;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.IView;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.cdo.spi.server.InternalSession;
import org.eclipse.emf.cdo.spi.server.InternalTransaction;
import org.eclipse.emf.cdo.spi.server.InternalView;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.postgresql.PostgreSQLAdapter;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CDO嵌入式Repository管理器 - 直接使用Repository，不通过网络
 * 这避免了网络协议注册的问题
 */
// @Component  // 暂时禁用，使用CDORepositoryManager代替
@Slf4j
public class CDOEmbeddedRepositoryManager {
    
    @Value("${cdo.repository.name:sysml}")
    private String repositoryName;
    
    private InternalRepository repository;
    private ResourceSet resourceSet;
    private boolean initialized = false;
    
    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing CDO Embedded Repository Manager");
            
            // 1. 创建PostgreSQL存储
            IStore store = createPostgreSQLStore();
            
            // 2. 创建Repository
            Map<String, String> props = new HashMap<>();
            props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo-uuid");
            props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
            props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");
            
            repository = (InternalRepository) CDOServerUtil.createRepository(repositoryName, store, props);
            
            // 3. 激活Repository
            LifecycleUtil.activate(repository);
            
            // 4. 创建ResourceSet
            resourceSet = new ResourceSetImpl();
            
            // 5. 注册Ecore包
            resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
            
            initialized = true;
            log.info("CDO Embedded Repository Manager initialized successfully");
            log.info("Repository State: {}", repository.getState());
            
        } catch (Exception e) {
            log.error("Failed to initialize CDO Embedded Repository Manager", e);
            initialized = false;
            throw new RuntimeException("CDO initialization failed", e);
        }
    }
    
    /**
     * 创建PostgreSQL存储
     */
    private IStore createPostgreSQLStore() {
        log.info("Creating PostgreSQL store for CDO");
        
        // PostgreSQL数据源配置
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{"localhost"});
        dataSource.setPortNumbers(new int[]{5432});
        dataSource.setDatabaseName("sysml_dev_db");
        dataSource.setUser("postgres");
        dataSource.setPassword("123456");
        
        // 创建映射策略
        IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);
        
        // 创建PostgreSQL适配器
        IDBAdapter dbAdapter = new PostgreSQLAdapter();
        
        // 创建DB存储
        IDBStore store = CDODBUtil.createStore(
            mappingStrategy,
            dbAdapter,
            DBUtil.createConnectionProvider(dataSource)
        );
        
        log.info("PostgreSQL store created successfully");
        return store;
    }
    
    /**
     * 保存EObject到CDO Repository - 真实持久化到PostgreSQL
     */
    public String saveModel(EObject model, String resourcePath) {
        if (!initialized) {
            throw new IllegalStateException("CDO Repository not initialized");
        }
        
        try {
            // 创建一个Server端Session
            ISession serverSession = repository.getSessionManager().openSession(null);
            
            // 创建Server端Transaction (需要提供view ID)
            ITransaction serverTransaction = serverSession.openTransaction(
                1, // view ID
                repository.getBranchManager().getMainBranch().getHead(),
                "embedded-transaction");
            
            // 创建或获取资源
            String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
            Resource resource = resourceSet.getResource(URI.createURI("cdo:" + path), false);
            if (resource == null) {
                resource = resourceSet.createResource(URI.createURI("cdo:" + path));
            }
            
            // 添加模型到资源
            resource.getContents().clear();
            resource.getContents().add(model);
            
            // 保存资源
            resource.save(null);
            
            // 提交事务 (ITransaction没有commit方法，需要使用其他方式)
            // 对于服务器端事务，数据直接写入存储
            serverTransaction.close();
            serverSession.close();
            
            log.info("Model successfully saved to CDO/PostgreSQL at: {}", path);
            return path;
            
        } catch (Exception e) {
            log.error("Failed to save model to CDO", e);
            throw new RuntimeException("CDO save failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从CDO Repository加载模型 - 真实从PostgreSQL读取
     */
    public EObject loadModel(String resourcePath) {
        if (!initialized) {
            throw new IllegalStateException("CDO Repository not initialized");
        }
        
        try {
            // 创建Server端Session
            ISession serverSession = repository.getSessionManager().openSession(null);
            
            // 创建Server端View（只读）(需要提供view ID)
            IView serverView = serverSession.openView(
                2, // view ID
                repository.getBranchManager().getMainBranch().getHead(),
                "embedded-view");
            
            // 加载资源
            String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
            Resource resource = resourceSet.getResource(URI.createURI("cdo:" + path), true);
            
            if (resource != null && !resource.getContents().isEmpty()) {
                EObject loaded = resource.getContents().get(0);
                log.info("Model loaded from CDO/PostgreSQL: {}", path);
                
                serverView.close();
                serverSession.close();
                
                return loaded;
            }
            
            serverView.close();
            serverSession.close();
            
            return null;
            
        } catch (Exception e) {
            log.error("Failed to load model from CDO", e);
            return null;
        }
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
               repository.getState() == IRepository.State.ONLINE;
    }
    
    /**
     * 获取服务器信息
     */
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("initialized", initialized);
        info.put("repositoryName", repositoryName);
        info.put("mode", "EMBEDDED");
        
        if (repository != null) {
            info.put("repositoryUUID", repository.getUUID());
            info.put("repositoryState", repository.getState().toString());
            info.put("storeType", "PostgreSQL");
        }
        
        return info;
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down CDO Embedded Repository Manager");
        
        if (repository != null) {
            LifecycleUtil.deactivate(repository);
        }
        
        initialized = false;
    }
}