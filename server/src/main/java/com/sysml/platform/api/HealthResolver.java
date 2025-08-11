package com.sysml.platform.api;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.Map;
import java.util.HashMap;

/**
 * RQ-API-ENDPOINT-001: Health查询解析器
 * 提供基本的健康检查功能
 */
@Controller
public class HealthResolver {

    @QueryMapping
    public boolean ok() {
        return true;
    }

    @QueryMapping
    public Map<String, Object> health(@Argument String component) {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", "1.0.0");
        if (component != null) {
            health.put("component", component);
        }
        return health;
    }
}