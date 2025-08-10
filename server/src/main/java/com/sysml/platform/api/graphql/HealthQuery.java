package com.sysml.platform.api.graphql;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.Map;

/**
 * GraphQL健康检查
 */
@Controller
public class HealthQuery {
    
    @QueryMapping
    public boolean ok() {
        return true;
    }
    
    @QueryMapping
    public HealthStatus health() {
        return HealthStatus.builder()
            .status("UP")
            .components(Map.of(
                "cdo", "UP",
                "sirius", "UP",
                "database", "UP"
            ))
            .build();
    }
}