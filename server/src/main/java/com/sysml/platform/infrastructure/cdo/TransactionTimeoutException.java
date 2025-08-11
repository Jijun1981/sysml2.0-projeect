package com.sysml.platform.infrastructure.cdo;

/**
 * 事务超时异常
 */
public class TransactionTimeoutException extends RuntimeException {
    
    public TransactionTimeoutException(String message) {
        super(message);
    }
    
    public TransactionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}