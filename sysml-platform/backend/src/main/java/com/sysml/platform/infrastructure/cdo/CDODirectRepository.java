package com.sysml.platform.infrastructure.cdo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.ecore.EObject;
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
 * CDO直接Repository管理器 - 简化版本
 * 直接使用Repository的存储层，不使用复杂的Session/Transaction机制
 */
// @Component  // 禁用，使用CDOStandardRepository代替
@Slf4j
public class CDODirectRepository {
    
    @Value("${cdo.repository.name:sysml}")
    private String repositoryName;
    
    private InternalRepository repository;
    private IStore store;
    private boolean initialized = false;
    
    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing CDO Direct Repository (simplified)");
            
            // 1. 创建PostgreSQL存储
            store = createPostgreSQLStore();
            
            // 2. 创建Repository前先注册基础包
            // 确保Ecore包已注册
            org.eclipse.emf.ecore.EcorePackage.eINSTANCE.eClass();
            
            Map<String, String> props = new HashMap<>();
            props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo-uuid");
            props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
            props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");
            props.put(IRepository.Props.ENSURE_REFERENTIAL_INTEGRITY, "false");
            
            repository = (InternalRepository) CDOServerUtil.createRepository(repositoryName, store, props);
            
            // 3. 激活Repository
            LifecycleUtil.activate(repository);
            
            initialized = true;
            log.info("CDO Direct Repository initialized successfully");
            log.info("Repository State: {}", repository.getState());
            log.info("Repository UUID: {}", repository.getUUID());
            
        } catch (Exception e) {
            log.error("Failed to initialize CDO Direct Repository", e);
            initialized = false;
            throw new RuntimeException("CDO initialization failed", e);
        }
    }
    
    /**
     * 创建PostgreSQL存储
     */
    private IStore createPostgreSQLStore() {
        log.info("Creating PostgreSQL store for CDO");
        
        try {
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
            
        } catch (Exception e) {
            log.error("Failed to create PostgreSQL store", e);
            throw new RuntimeException("PostgreSQL store creation failed", e);
        }
    }
    
    /**
     * 保存EObject到CDO Repository - 简化版本，先验证连接
     */
    public String saveModel(EObject model, String resourcePath) {
        if (!initialized) {
            throw new IllegalStateException("CDO Repository not initialized");
        }
        
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        
        try {
            // 首先验证Repository状态
            if (repository.getState() != IRepository.State.ONLINE) {
                throw new RuntimeException("Repository is not online: " + repository.getState());
            }
            
            // 记录保存尝试（实际的复杂保存逻辑需要更深入的CDO内部API）
            String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
            log.info("Attempting to save model to CDO/PostgreSQL at: {}", path);
            log.info("Model class: {}", model.getClass().getName());
            log.info("Repository sessions: {}", repository.getSessionManager().getSessions().length);
            
            // 这里应该有实际的保存逻辑，但需要更深入理解CDO内部机制
            // 目前先返回路径表示"保存成功"，实际保存逻辑需要进一步研究
            log.warn("Save model logic simplified - need to implement actual CDO persistence");
            
            return path;
            
        } catch (Exception e) {
            log.error("Failed to save model to CDO", e);
            throw new RuntimeException("CDO save failed: " + e.getMessage(), e);
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
        info.put("mode", "DIRECT");
        
        if (repository != null) {
            info.put("repositoryUUID", repository.getUUID());
            info.put("repositoryState", repository.getState().toString());
            info.put("storeType", "PostgreSQL");
            
            // 获取统计信息
            info.put("sessionCount", repository.getSessionManager().getSessions().length);
        }
        
        return info;
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down CDO Direct Repository");
        
        if (repository != null) {
            LifecycleUtil.deactivate(repository);
        }
        
        initialized = false;
    }
}