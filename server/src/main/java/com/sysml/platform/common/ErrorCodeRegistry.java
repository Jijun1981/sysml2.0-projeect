package com.sysml.platform.common;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * RQ-NFR-ERROR-004: 错误码注册表
 * 管理错误码到消息键的映射
 */
@Component
public class ErrorCodeRegistry {
    
    private final Map<String, String> codeToMessageKey = new ConcurrentHashMap<>();
    
    public ErrorCodeRegistry() {
        // 初始化默认错误码
        for (ErrorCode errorCode : ErrorCode.values()) {
            register(errorCode.getCode(), errorCode.getMessageKey());
        }
    }
    
    /**
     * 注册错误码
     */
    public void register(String code, String messageKey) {
        codeToMessageKey.put(code, messageKey);
    }
    
    /**
     * 获取消息键
     */
    public String getMessageKey(String code) {
        return codeToMessageKey.get(code);
    }
    
    /**
     * 检查错误码是否已注册
     */
    public boolean isRegistered(String code) {
        return codeToMessageKey.containsKey(code);
    }
}