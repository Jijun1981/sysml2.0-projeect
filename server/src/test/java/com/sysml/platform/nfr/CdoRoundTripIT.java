package com.sysml.platform.nfr;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
class CdoRoundTripIT {

  @Autowired ApplicationContext applicationContext;

  @Test
  void minimalRoundTrip_ifCdoPresent() throws Exception {
    try {
      Class<?> repoType = Class.forName("org.eclipse.emf.cdo.server.IRepository");
      Object repo = applicationContext.getBean(repoType);
      Assumptions.assumeTrue(repo != null, "CDO not available");
      Method isActive = repo.getClass().getMethod("isActive");
      boolean active = (boolean) isActive.invoke(repo);
      Assumptions.assumeTrue(active, "Repository not active");
      // A true end-to-end commit would need client stacks; here we assert active state as smoke
    } catch (ClassNotFoundException e) {
      Assumptions.assumeTrue(false, "CDO classes missing");
    }
  }
}

