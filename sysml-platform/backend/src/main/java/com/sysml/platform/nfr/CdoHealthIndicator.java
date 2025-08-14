package com.sysml.platform.nfr;

import com.sysml.platform.infrastructure.cdo.CDOServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** CDO健康检查指示器 - 检查CDO服务器状态 */
@Component("cdo")
public class CdoHealthIndicator implements HealthIndicator {

  @Autowired(required = false)
  private CDOServerManager cdoServerManager;

  @Override
  public Health health() {
    if (cdoServerManager == null) {
      return Health.down().withDetail("status", "CDO Server not configured").build();
    }

    try {
      if (cdoServerManager.isHealthy()) {
        return Health.up()
            .withDetail("status", "CDO Server active")
            .withDetails(cdoServerManager.getServerInfo())
            .build();
      } else {
        return Health.down()
            .withDetail("status", "CDO Server not initialized")
            .withDetails(cdoServerManager.getServerInfo())
            .build();
      }
    } catch (Exception e) {
      return Health.down(e).withDetail("status", "CDO health check failed").build();
    }
  }
}
