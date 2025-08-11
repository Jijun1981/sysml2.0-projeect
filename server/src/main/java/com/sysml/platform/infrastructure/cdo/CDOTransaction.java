package com.sysml.platform.infrastructure.cdo;

import java.util.*;
import java.util.concurrent.*;

/**
 * CDO事务实现
 * 实现RQ-INFRA-TX-002: 事务边界管理
 */
public class CDOTransaction {
    
    private final CDOTransactionManager manager;
    private final Map<String, CDOResource> localChanges = new HashMap<>();
    private final Set<String> createdResources = new HashSet<>();
    private boolean active = true;
    private final long timeout;
    private final long startTime;
    
    public CDOTransaction(CDOTransactionManager manager) {
        this(manager, Long.MAX_VALUE);
    }
    
    public CDOTransaction(CDOTransactionManager manager, long timeoutMillis) {
        this.manager = manager;
        this.timeout = timeoutMillis;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * 事务是否活跃
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * 执行事务操作
     */
    public void execute(Runnable operation) {
        if (!active) {
            throw new IllegalStateException("Transaction is not active");
        }
        
        try {
            checkTimeout();
            operation.run();
        } catch (TransactionTimeoutException e) {
            rollback();
            throw e; // 直接抛出超时异常
        } catch (Exception e) {
            rollback();
            throw new RuntimeException("Transaction failed", e);
        }
    }
    
    /**
     * 创建资源
     */
    public CDOResource createResource(String path) {
        checkTimeout();
        CDOResource resource = new CDOResource(path);
        localChanges.put(path, resource);
        createdResources.add(path);
        return resource;
    }
    
    /**
     * 获取或创建资源
     */
    public CDOResource getOrCreateResource(String path) {
        checkTimeout();
        CDOResource resource = localChanges.get(path);
        if (resource == null) {
            resource = manager.getResource(path);
            if (resource == null) {
                resource = createResource(path);
            }
        }
        return resource;
    }
    
    /**
     * 提交事务
     */
    public boolean commit() {
        if (!active) {
            return false;
        }
        
        try {
            checkTimeout();
            
            // 持久化所有更改
            for (Map.Entry<String, CDOResource> entry : localChanges.entrySet()) {
                manager.saveResource(entry.getKey(), entry.getValue());
            }
            
            active = false;
            return true;
        } catch (Exception e) {
            rollback();
            return false;
        }
    }
    
    /**
     * 回滚事务
     */
    public boolean rollback() {
        if (!active) {
            return false;
        }
        
        // 清理本地更改，不持久化
        localChanges.clear();
        createdResources.clear();
        active = false;
        return true;
    }
    
    /**
     * 检查超时
     */
    private void checkTimeout() {
        if (System.currentTimeMillis() - startTime > timeout) {
            rollback();
            throw new TransactionTimeoutException("Transaction timeout after " + timeout + "ms");
        }
    }
}