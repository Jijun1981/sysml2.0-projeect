package com.sysml.platform.infrastructure.cdo;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CDO事务管理器
 * 实现RQ-INFRA-TX-002: 事务边界管理
 * 
 * @implements RQ-INFRA-TX-002
 */
@Component
public class CDOTransactionManager {
    
    private final Map<String, Object> resources = new ConcurrentHashMap<>();
    private final ThreadLocal<CDOTransaction> currentTransaction = new ThreadLocal<>();
    
    /**
     * 开始新事务
     */
    public CDOTransaction beginTransaction() {
        CDOTransaction transaction = new CDOTransaction(this);
        currentTransaction.set(transaction);
        return transaction;
    }
    
    /**
     * 开始带超时的事务
     */
    public CDOTransaction beginTransaction(long timeoutMillis) {
        CDOTransaction transaction = new CDOTransaction(this, timeoutMillis);
        currentTransaction.set(transaction);
        return transaction;
    }
    
    /**
     * 检查资源是否存在
     */
    public boolean resourceExists(String path) {
        return resources.containsKey(path);
    }
    
    /**
     * 获取资源
     */
    public CDOResource getResource(String path) {
        return (CDOResource) resources.get(path);
    }
    
    /**
     * 内部方法：保存资源
     */
    void saveResource(String path, CDOResource resource) {
        resources.put(path, resource);
    }
    
    /**
     * 内部方法：删除资源
     */
    void removeResource(String path) {
        resources.remove(path);
    }
}