package com.sysml.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** 真实的Sirius Web启动器 集成真实的Sirius Web组件，不是mock */
@SpringBootApplication
@ComponentScan(
    basePackages = {
      "com.sysml.platform",
      "org.eclipse.sirius.web" // 扫描Sirius Web组件
    })
public class SiriusWebRunner {

  public static void main(String[] args) {
    System.setProperty("spring.profiles.active", "sirius");
    SpringApplication.run(SiriusWebRunner.class, args);
  }
}
