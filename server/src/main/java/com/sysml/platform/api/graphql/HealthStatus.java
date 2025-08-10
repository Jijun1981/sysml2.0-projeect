package com.sysml.platform.api.graphql;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class HealthStatus {
    private String status;
    private Map<String, Object> components;
}