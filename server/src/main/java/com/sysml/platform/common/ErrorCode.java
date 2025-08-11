package com.sysml.platform.common;

/**
 * RQ-NFR-ERROR-004: 错误码枚举
 * 统一的错误码定义
 */
public enum ErrorCode {
    // 客户端错误 (4xx)
    VALIDATION_ERROR("VALIDATION_ERROR", "error.validation.failed", "CLIENT"),
    NOT_FOUND("NOT_FOUND", "error.resource.notfound", "CLIENT"),
    BAD_REQUEST("BAD_REQUEST", "error.bad.request", "CLIENT"),
    
    // 认证授权错误
    UNAUTHORIZED("UNAUTHORIZED", "error.auth.unauthorized", "AUTH"),
    FORBIDDEN("FORBIDDEN", "error.auth.forbidden", "AUTH"),
    
    // 业务错误
    REQ_CYCLE_DETECTED("REQ_CYCLE_DETECTED", "error.requirement.cycle", "BUSINESS"),
    CONNECTION_INVALID("CONNECTION_INVALID", "error.connection.invalid", "BUSINESS"),
    
    // 系统错误 (5xx)
    INTERNAL_ERROR("INTERNAL_ERROR", "error.internal", "SYSTEM"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "error.service.unavailable", "SYSTEM");
    
    private final String code;
    private final String messageKey;
    private final String category;
    
    ErrorCode(String code, String messageKey, String category) {
        this.code = code;
        this.messageKey = messageKey;
        this.category = category;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessageKey() {
        return messageKey;
    }
    
    public String getCategory() {
        return category;
    }
}