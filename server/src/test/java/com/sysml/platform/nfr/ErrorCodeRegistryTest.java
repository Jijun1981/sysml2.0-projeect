package com.sysml.platform.nfr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.sysml.platform.common.ErrorCode;
import com.sysml.platform.common.ErrorCodeRegistry;

/**
 * RQ-NFR-ERROR-004: 错误码注册
 * 验收条件：code→messageKey映射
 */
@SpringBootTest
public class ErrorCodeRegistryTest {

    @Autowired(required = false)
    private ErrorCodeRegistry errorCodeRegistry;

    @Test
    @DisplayName("错误码应该正确映射到消息键")
    public void shouldMapErrorCodesToMessageKeys() {
        // Given: 错误码注册表
        if (errorCodeRegistry == null) {
            errorCodeRegistry = new ErrorCodeRegistry();
        }

        // When: 注册错误码
        errorCodeRegistry.register("VALIDATION_ERROR", "error.validation.failed");
        errorCodeRegistry.register("NOT_FOUND", "error.resource.notfound");
        errorCodeRegistry.register("UNAUTHORIZED", "error.auth.unauthorized");

        // Then: 验证映射
        assertEquals("error.validation.failed", errorCodeRegistry.getMessageKey("VALIDATION_ERROR"));
        assertEquals("error.resource.notfound", errorCodeRegistry.getMessageKey("NOT_FOUND"));
        assertEquals("error.auth.unauthorized", errorCodeRegistry.getMessageKey("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("应该支持错误码枚举")
    public void shouldSupportErrorCodeEnum() {
        // Given: 错误码枚举
        ErrorCode validationError = ErrorCode.VALIDATION_ERROR;
        ErrorCode notFound = ErrorCode.NOT_FOUND;

        // Then: 验证枚举值
        assertEquals("VALIDATION_ERROR", validationError.getCode());
        assertEquals("error.validation.failed", validationError.getMessageKey());
        
        assertEquals("NOT_FOUND", notFound.getCode());
        assertEquals("error.resource.notfound", notFound.getMessageKey());
    }

    @Test
    @DisplayName("应该支持错误码分类")
    public void shouldSupportErrorCodeCategories() {
        // Given: 错误码
        ErrorCode validationError = ErrorCode.VALIDATION_ERROR;
        ErrorCode authError = ErrorCode.UNAUTHORIZED;
        ErrorCode systemError = ErrorCode.INTERNAL_ERROR;

        // Then: 验证分类
        assertEquals("CLIENT", validationError.getCategory());
        assertEquals("AUTH", authError.getCategory());
        assertEquals("SYSTEM", systemError.getCategory());
    }
}