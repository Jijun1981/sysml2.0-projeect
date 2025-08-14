package com.sysml.platform;

import com.sysml.platform.infrastructure.cdo.LocalEcoreLoader;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** SysML v2 建模平台启动类 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    // Register local Ecore packages from resources
    LocalEcoreLoader.registerLocalEcorePackagesOrThrow(
        List.of("ecore/sysml/SysML.ecore", "ecore/sysml/kerml.ecore", "ecore/sysml/types.ecore"));
  }
}
