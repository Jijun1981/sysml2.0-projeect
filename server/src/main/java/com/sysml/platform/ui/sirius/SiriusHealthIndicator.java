package com.sysml.platform.ui.sirius;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RQ-UI-RUNTIME-001: Sirius运行时健康检查
 * 监控Sirius运行时状态
 */
@Component("sirius")
public class SiriusHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private SiriusRuntime siriusRuntime;

    @Override
    public Health health() {
        if (siriusRuntime == null || !siriusRuntime.isRunning()) {
            return Health.down()
                .withDetail("runtime", "Not started")
                .withDetail("backend", "CDO")
                .build();
        }

        return Health.up()
            .withDetail("runtime", "Sirius Web")
            .withDetail("backend", "CDO")
            .withDetail("version", siriusRuntime.getVersion())
            .withDetail("sessions", siriusRuntime.getActiveSessions())
            .build();
    }
}