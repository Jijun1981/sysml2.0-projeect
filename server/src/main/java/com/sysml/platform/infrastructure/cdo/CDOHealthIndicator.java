package com.sysml.platform.infrastructure.cdo;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** CDO健康检查指示器 实现RQ-INFRA-CDO-001: CDO健康与配置 */
@Component("cdo")
public class CDOHealthIndicator implements HealthIndicator {

  @Override
  public Health health() {
    // TODO: 实际检查CDO连接状态
    // 现在先返回UP状态
    return Health.up().withDetail("repository", "sysml-repo").withDetail("mode", "lean").build();
  }
}
