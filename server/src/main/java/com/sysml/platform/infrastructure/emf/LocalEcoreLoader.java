package com.sysml.platform.infrastructure.emf;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

/** 本地Ecore加载器 - 从classpath加载Ecore文件 满足RQ-M2-REG-001: EPackage注册需求 */
@Slf4j
public class LocalEcoreLoader {

  /** 注册本地Ecore包 */
  public static void registerLocalEcorePackagesOrThrow(List<String> ecorePaths) {
    log.info("Registering local Ecore packages from classpath...");

    ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("ecore", new EcoreResourceFactoryImpl());

    for (String ecorePath : ecorePaths) {
      try {
        registerEcorePackage(resourceSet, ecorePath);
      } catch (Exception e) {
        log.error("Failed to register Ecore package from: {}", ecorePath, e);
        throw new RuntimeException("Failed to register Ecore packages", e);
      }
    }

    log.info("Successfully registered {} Ecore packages", ecorePaths.size());
  }

  private static void registerEcorePackage(ResourceSet resourceSet, String ecorePath)
      throws IOException {
    log.debug("Loading Ecore file: {}", ecorePath);

    // Load from classpath
    InputStream inputStream =
        LocalEcoreLoader.class.getClassLoader().getResourceAsStream(ecorePath);

    if (inputStream == null) {
      throw new IOException("Ecore file not found in classpath: " + ecorePath);
    }

    // Create resource
    URI uri = URI.createURI("platform:/resource/" + ecorePath);
    Resource resource = resourceSet.createResource(uri);
    resource.load(inputStream, null);

    // Register packages
    resource
        .getContents()
        .forEach(
            content -> {
              if (content instanceof EPackage) {
                EPackage ePackage = (EPackage) content;
                String nsURI = ePackage.getNsURI();

                if (nsURI != null && !nsURI.isEmpty()) {
                  EPackage.Registry.INSTANCE.put(nsURI, ePackage);
                  log.info("Registered EPackage: {} with URI: {}", ePackage.getName(), nsURI);

                  // Register sub-packages
                  registerSubPackages(ePackage);
                }
              }
            });

    inputStream.close();
  }

  private static void registerSubPackages(EPackage parentPackage) {
    for (EPackage subPackage : parentPackage.getESubpackages()) {
      String nsURI = subPackage.getNsURI();
      if (nsURI != null && !nsURI.isEmpty()) {
        EPackage.Registry.INSTANCE.put(nsURI, subPackage);
        log.debug("Registered sub-package: {} with URI: {}", subPackage.getName(), nsURI);

        // Recursively register sub-packages
        registerSubPackages(subPackage);
      }
    }
  }
}
