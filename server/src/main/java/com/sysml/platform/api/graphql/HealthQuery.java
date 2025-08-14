package com.sysml.platform.api.graphql;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL健康检查 - 聚合实际健康状态 */
@Controller
@RequiredArgsConstructor
public class HealthQuery {

  private final HealthEndpoint healthEndpoint;

  @QueryMapping
  public boolean ok() {
    // 检查整体健康状态
    var health = healthEndpoint.health();
    return Status.UP.equals(health.getStatus());
  }

  @QueryMapping
  public HealthStatus health() {
    var healthComponent = healthEndpoint.health();
    var components = extractComponentStatus(healthComponent);

    return HealthStatus.builder()
        .status(healthComponent.getStatus().getCode())
        .components(components)
        .build();
  }

  /** 从HealthComponent对象中提取各组件状态 */
  @SuppressWarnings("unchecked")
  private Map<String, Object> extractComponentStatus(
      org.springframework.boot.actuate.health.HealthComponent healthComponent) {
    Map<String, Object> components = new HashMap<>();

    // HealthComponent可能是SystemHealth或CompositeHealth
    if (healthComponent instanceof org.springframework.boot.actuate.health.SystemHealth) {
      var systemHealth = (org.springframework.boot.actuate.health.SystemHealth) healthComponent;
      var componentMap = systemHealth.getComponents();
      if (componentMap != null) {
        componentMap.forEach(
            (key, component) -> {
              components.put(key, component.getStatus().getCode());
            });
      }
    } else if (healthComponent instanceof org.springframework.boot.actuate.health.CompositeHealth) {
      var compositeHealth =
          (org.springframework.boot.actuate.health.CompositeHealth) healthComponent;
      var componentMap = compositeHealth.getComponents();
      if (componentMap != null) {
        componentMap.forEach(
            (key, component) -> {
              components.put(key, component.getStatus().getCode());
            });
      }
    }

    // 确保关键组件都有状态（兼容不同命名）
    if (!components.containsKey("cdo")) {
      components.put("cdo", "UNKNOWN");
    }
    if (!components.containsKey("db") && !components.containsKey("database")) {
      components.put("database", "UNKNOWN");
    }
    if (!components.containsKey("sirius")) {
      components.put("sirius", "UNKNOWN");
    }

    return components;
  }
}
