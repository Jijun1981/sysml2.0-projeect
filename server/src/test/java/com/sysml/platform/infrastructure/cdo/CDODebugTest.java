package com.sysml.platform.infrastructure.cdo;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.cdo.server.IRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * CDO调试测试 - 诊断CDO初始化问题
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/sysml_test_db",
    "spring.datasource.username=sysml_user",
    "spring.datasource.password=sysml_password",
    "cdo.enabled=true",
    "cdo.repository.name=sysml-test",
    "cdo.server.port=2037",
    "cdo.store.drop-on-activate=true",
    "logging.level.com.sysml.platform.infrastructure.cdo=DEBUG",
    "logging.level.org.eclipse.emf.cdo=DEBUG",
    "logging.level.org.eclipse.net4j=DEBUG"
})
class CDODebugTest {

  @Autowired(required = false)
  private CDOServerManager cdoServerManager;

  @Test
  void debugCDOInitialization() {
    System.out.println("========== CDO Debug Test ==========");
    
    // 检查CDOServerManager是否注入
    if (cdoServerManager == null) {
      System.out.println("CDOServerManager is NULL - not injected!");
      assertThat(cdoServerManager).isNotNull();
      return;
    }
    
    System.out.println("CDOServerManager injected: " + cdoServerManager.getClass().getName());
    
    // 检查Repository状态
    IRepository repository = cdoServerManager.getRepository();
    if (repository == null) {
      System.out.println("Repository is NULL!");
    } else {
      System.out.println("Repository Name: " + repository.getName());
      System.out.println("Repository UUID: " + repository.getUUID());
      System.out.println("Repository State: " + repository.getState());
      System.out.println("Repository is Active: " + repository.isActive());
    }
    
    // 检查Session状态
    var session = cdoServerManager.getSession();
    if (session == null) {
      System.out.println("Session is NULL!");
    } else {
      System.out.println("Session ID: " + session.getSessionID());
      System.out.println("Session is Closed: " + session.isClosed());
      System.out.println("Session Repository Name: " + session.getRepositoryInfo().getName());
    }
    
    // 检查健康状态
    boolean healthy = cdoServerManager.isHealthy();
    System.out.println("CDO Server is Healthy: " + healthy);
    
    // 获取详细信息
    var info = cdoServerManager.getServerInfo();
    System.out.println("\n=== Server Info ===");
    info.forEach((key, value) -> System.out.println(key + ": " + value));
    
    System.out.println("========== End CDO Debug ==========");
    
    // 如果不健康，让测试失败以查看日志
    if (!healthy) {
      System.out.println("\n!!! CDO Server is NOT healthy !!!");
      System.out.println("Please check the logs above for initialization errors.");
    }
    
    assertThat(healthy).isTrue();
  }
}