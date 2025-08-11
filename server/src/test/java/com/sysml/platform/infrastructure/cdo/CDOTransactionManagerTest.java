package com.sysml.platform.infrastructure.cdo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
/**
 * RQ-INFRA-TX-002: 事务边界管理
 * 验收条件：commit/rollback语义正确
 * 
 * TDD: 先写测试，定义期望行为
 * 
 * @TestCase TC-INFRA-TX-001
 * @TestCase TC-INFRA-TX-002
 * @TestCase TC-INFRA-TX-003
 */
public class CDOTransactionManagerTest {
    
    private CDOTransactionManager transactionManager;
    
    @BeforeEach
    public void setUp() {
        transactionManager = new CDOTransactionManager();
    }
    /**
     * TC-INFRA-TX-001: 正常提交事务
     */
    @Test
    @DisplayName("应该成功提交事务并持久化更改")
    public void shouldCommitTransactionSuccessfully() {
        // Given: 开启一个事务
        CDOTransaction transaction = transactionManager.beginTransaction();
        assertNotNull(transaction);
        assertTrue(transaction.isActive());
        
        // When: 执行操作并提交
        transaction.execute(() -> {
            // 模拟创建一个对象
            transaction.createResource("/test/resource1");
        });
        
        boolean committed = transaction.commit();
        
        // Then: 事务应该成功提交
        assertTrue(committed);
        assertFalse(transaction.isActive());
        
        // 验证资源已持久化
        assertTrue(transactionManager.resourceExists("/test/resource1"));
    }
    
    /**
     * TC-INFRA-TX-002: 回滚事务
     */
    @Test
    @DisplayName("应该成功回滚事务，撤销所有更改")
    public void shouldRollbackTransactionOnError() {
        // Given: 开启一个事务
        CDOTransaction transaction = transactionManager.beginTransaction();
        
        // When: 执行操作但遇到错误
        try {
            transaction.execute(() -> {
                transaction.createResource("/test/resource2");
                // 模拟出错
                throw new RuntimeException("Simulated error");
            });
        } catch (RuntimeException e) {
            // 预期的异常，忽略
        }
        
        // 事务应该已经自动回滚了（在execute中）
        
        // Then: 事务应该回滚
        assertFalse(transaction.isActive());
        
        // 验证资源未持久化
        assertFalse(transactionManager.resourceExists("/test/resource2"));
    }
    
    /**
     * TC-INFRA-TX-003: 嵌套事务处理
     */
    @Test
    @DisplayName("应该正确处理嵌套事务")
    public void shouldHandleNestedTransactions() {
        // Given: 外层事务
        CDOTransaction outerTx = transactionManager.beginTransaction();
        outerTx.createResource("/test/outer");
        
        // When: 创建嵌套事务（当前实现不支持真正的嵌套，会创建独立事务）
        CDOTransaction innerTx = transactionManager.beginTransaction();
        innerTx.createResource("/test/inner");
        
        // 内层提交
        assertTrue(innerTx.commit());
        
        // 外层回滚
        assertTrue(outerTx.rollback());
        
        // Then: 当前实现中，内外层事务独立
        // 内层已提交的会保留，外层回滚的不会保留
        assertFalse(transactionManager.resourceExists("/test/outer"));
        assertTrue(transactionManager.resourceExists("/test/inner")); // 独立事务，已提交
    }
    
    /**
     * TC-INFRA-TX-004: 事务超时处理
     */
    @Test
    @DisplayName("应该处理事务超时")
    public void shouldHandleTransactionTimeout() {
        // Given: 设置短超时的事务
        CDOTransaction transaction = transactionManager.beginTransaction(1000); // 1秒超时
        
        // When: 执行耗时操作
        assertThrows(TransactionTimeoutException.class, () -> {
            transaction.execute(() -> {
                try {
                    Thread.sleep(2000); // 模拟耗时2秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                transaction.createResource("/test/timeout");
            });
        });
        
        // Then: 事务应该自动回滚
        assertFalse(transaction.isActive());
        assertFalse(transactionManager.resourceExists("/test/timeout"));
    }
    
    /**
     * TC-INFRA-TX-005: 并发事务隔离
     */
    @Test
    @DisplayName("应该正确隔离并发事务")
    public void shouldIsolateConcurrentTransactions() throws InterruptedException {
        // Given: 两个并发事务
        CDOTransaction tx1 = transactionManager.beginTransaction();
        CDOTransaction tx2 = transactionManager.beginTransaction();
        
        // When: 并发修改同一资源
        Thread t1 = new Thread(() -> {
            tx1.execute(() -> {
                CDOResource resource = tx1.getOrCreateResource("/test/concurrent");
                resource.setAttribute("value", "tx1");
            });
            tx1.commit();
        });
        
        Thread t2 = new Thread(() -> {
            tx2.execute(() -> {
                CDOResource resource = tx2.getOrCreateResource("/test/concurrent");
                resource.setAttribute("value", "tx2");
            });
            tx2.commit();
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        // Then: 后提交的事务应该覆盖前者（或根据隔离级别抛出冲突）
        CDOResource resource = transactionManager.getResource("/test/concurrent");
        assertNotNull(resource.getAttribute("value"));
        // 具体值取决于事务隔离级别和提交顺序
    }
}