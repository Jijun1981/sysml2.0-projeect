package com.sysml.platform.infrastructure.transaction;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/** 事务管理器 - 满足RQ-INFRA-TX-002需求 提供事务边界管理和验证 */
@Component("sysmlTransactionManager")
@RequiredArgsConstructor
@Slf4j
public class TransactionManager {

  private final PlatformTransactionManager platformTransactionManager;
  private TransactionTemplate transactionTemplate;

  private final AtomicInteger successCount = new AtomicInteger(0);
  private final AtomicInteger rollbackCount = new AtomicInteger(0);

  @PostConstruct
  public void initialize() {
    transactionTemplate = new TransactionTemplate(platformTransactionManager);
    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    log.info("TransactionManager initialized");
  }

  /** 在事务中执行操作 */
  public <T> T executeInTransaction(TransactionCallback<T> action) {
    return transactionTemplate.execute(
        status -> {
          try {
            T result = action.doInTransaction(status);
            successCount.incrementAndGet();
            log.debug("Transaction committed successfully");
            return result;
          } catch (Exception e) {
            status.setRollbackOnly();
            rollbackCount.incrementAndGet();
            log.error("Transaction rolled back due to error", e);
            throw e;
          }
        });
  }

  /** 手动开始事务 */
  public TransactionStatus beginTransaction() {
    DefaultTransactionDefinition def = new DefaultTransactionDefinition();
    def.setName("ManualTransaction");
    def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

    TransactionStatus status = platformTransactionManager.getTransaction(def);
    log.debug("Transaction started manually");
    return status;
  }

  /** 提交事务 */
  public void commitTransaction(TransactionStatus status) {
    if (!status.isCompleted()) {
      platformTransactionManager.commit(status);
      successCount.incrementAndGet();
      log.debug("Transaction committed manually");
    }
  }

  /** 回滚事务 */
  public void rollbackTransaction(TransactionStatus status) {
    if (!status.isCompleted()) {
      platformTransactionManager.rollback(status);
      rollbackCount.incrementAndGet();
      log.debug("Transaction rolled back manually");
    }
  }

  /** 测试事务提交 */
  public boolean testCommit(Runnable operation) {
    try {
      return executeInTransaction(
          status -> {
            operation.run();
            return true;
          });
    } catch (Exception e) {
      return false;
    }
  }

  /** 测试事务回滚 */
  public boolean testRollback(Runnable operation) {
    try {
      return executeInTransaction(
          status -> {
            operation.run();
            throw new RuntimeException("Intentional rollback for testing");
          });
    } catch (RuntimeException e) {
      // 预期的异常，事务应该已回滚
      return true;
    }
  }

  /** 获取事务统计 */
  public TransactionStats getStats() {
    return new TransactionStats(successCount.get(), rollbackCount.get());
  }

  /** 事务统计 */
  public record TransactionStats(int commitCount, int rollbackCount) {
    public int totalTransactions() {
      return commitCount + rollbackCount;
    }

    public double successRate() {
      int total = totalTransactions();
      return total > 0 ? (double) commitCount / total * 100 : 0;
    }
  }
}
