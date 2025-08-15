package com.sysml.platform.api;

import com.sysml.platform.infrastructure.emf.EMFModelManager;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 统一健康检查控制器 满足RQ-NFR-HEALTH-001: /health聚合所有子系统 */
@RestController
@RequestMapping("/health")
public class HealthController implements HealthIndicator {

  @Autowired(required = false)
  private EMFModelManager emfModelManager;

  @Autowired(required = false)
  private DataSource dataSource;

  @Override
  public Health health() {
    Health.Builder builder = Health.up();
    Map<String, Object> details = new HashMap<>();

    // EMF检查
    if (emfModelManager != null) {
      details.put("emf", "UP");
      details.put("emf_message", "EMF Model Manager is running");
    } else {
      details.put("emf", "DOWN");
      details.put("emf_message", "EMF Model Manager not initialized");
    }

    // 数据库检查
    checkDatabase(details);

    builder.withDetails(details);
    return builder.build();
  }

  /** 聚合健康检查端点 */
  @GetMapping
  public ResponseEntity<Map<String, Object>> healthCheck() {
    Health health = health();
    Map<String, Object> response = new HashMap<>();
    response.put("status", health.getStatus().toString());
    response.put("details", health.getDetails());
    return ResponseEntity.ok(response);
  }

  /** EMF健康检查 */
  @GetMapping("/emf")
  public ResponseEntity<Map<String, String>> emfHealth() {
    Map<String, String> status = new HashMap<>();
    if (emfModelManager != null) {
      status.put("status", "UP");
      status.put("message", "EMF Model Manager is running");
    } else {
      status.put("status", "DOWN");
      status.put("message", "EMF Model Manager not initialized");
    }
    return ResponseEntity.ok(status);
  }

  /** 数据库健康检查 */
  @GetMapping("/database")
  public ResponseEntity<Map<String, String>> databaseHealth() {
    Map<String, String> status = new HashMap<>();
    checkDatabase(status);
    return ResponseEntity.ok(status);
  }

  private void checkDatabase(Map<String, ? super String> details) {
    if (dataSource == null) {
      details.put("database", "DOWN");
      details.put("database_message", "DataSource not configured");
      return;
    }

    try (Connection conn = dataSource.getConnection()) {
      if (conn.isValid(1)) {
        details.put("database", "UP");
        details.put("database_message", "PostgreSQL is running");
      } else {
        details.put("database", "DOWN");
        details.put("database_message", "Connection is not valid");
      }
    } catch (Exception e) {
      details.put("database", "DOWN");
      details.put("database_message", "Cannot connect: " + e.getMessage());
    }
  }
}
