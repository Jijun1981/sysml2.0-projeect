package com.sysml.platform.infrastructure.m2;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** M2模型注册管理器 负责加载和注册KerML/SysML的Ecore元模型 */
@Component
public class M2ModelRegistry {

  private static final Logger logger = LoggerFactory.getLogger(M2ModelRegistry.class);

  private ResourceSet resourceSet;
  private EPackage kermlPackage;
  private EPackage sysmlPackage;

  @PostConstruct
  public void initialize() {
    logger.info("Initializing M2 Model Registry...");

    // 创建ResourceSet
    resourceSet = new ResourceSetImpl();

    // 注册Ecore资源工厂
    resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("ecore", new EcoreResourceFactoryImpl());

    resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());

    // 加载模型
    loadKerMLModel();
    loadSysMLModel();

    logger.info("M2 Model Registry initialized successfully");
  }

  private void loadKerMLModel() {
    try {
      // 使用文件系统路径
      String kermlPath =
          getClass().getClassLoader().getResource("model/kerml.ecore").toURI().toString();
      URI kermlURI = URI.createURI(kermlPath);
      Resource kermlResource = resourceSet.getResource(kermlURI, true);

      if (!kermlResource.getContents().isEmpty()) {
        kermlPackage = (EPackage) kermlResource.getContents().get(0);

        // 注册到全局Registry
        EPackage.Registry.INSTANCE.put(kermlPackage.getNsURI(), kermlPackage);

        logger.info("KerML model loaded successfully. URI: {}", kermlPackage.getNsURI());
        logger.info("KerML EClassifiers count: {}", kermlPackage.getEClassifiers().size());
      }
    } catch (Exception e) {
      logger.error("Failed to load KerML model", e);
    }
  }

  private void loadSysMLModel() {
    try {
      // 使用文件系统路径
      String sysmlPath =
          getClass().getClassLoader().getResource("model/SysML.ecore").toURI().toString();
      URI sysmlURI = URI.createURI(sysmlPath);
      Resource sysmlResource = resourceSet.getResource(sysmlURI, true);

      if (!sysmlResource.getContents().isEmpty()) {
        sysmlPackage = (EPackage) sysmlResource.getContents().get(0);

        // 注册到全局Registry
        EPackage.Registry.INSTANCE.put(sysmlPackage.getNsURI(), sysmlPackage);

        logger.info("SysML model loaded successfully. URI: {}", sysmlPackage.getNsURI());
        logger.info("SysML EClassifiers count: {}", sysmlPackage.getEClassifiers().size());
      }
    } catch (Exception e) {
      logger.error("Failed to load SysML model", e);
    }
  }

  /** 获取已注册的所有包 */
  public Map<String, Object> getRegisteredPackages() {
    return EPackage.Registry.INSTANCE;
  }

  /** 获取KerML包 */
  public EPackage getKermlPackage() {
    return kermlPackage;
  }

  /** 获取SysML包 */
  public EPackage getSysmlPackage() {
    return sysmlPackage;
  }

  /** 验证模型是否加载成功 */
  public boolean isModelLoaded() {
    return kermlPackage != null && sysmlPackage != null;
  }

  /** 获取模型统计信息 */
  public String getModelStatistics() {
    StringBuilder stats = new StringBuilder();
    stats.append("M2 Model Statistics:\n");

    if (kermlPackage != null) {
      stats
          .append("  KerML: ")
          .append(kermlPackage.getEClassifiers().size())
          .append(" classifiers\n");
    }

    if (sysmlPackage != null) {
      stats
          .append("  SysML: ")
          .append(sysmlPackage.getEClassifiers().size())
          .append(" classifiers\n");
    }

    stats
        .append("  Total registered packages: ")
        .append(EPackage.Registry.INSTANCE.size())
        .append("\n");

    return stats.toString();
  }
}
