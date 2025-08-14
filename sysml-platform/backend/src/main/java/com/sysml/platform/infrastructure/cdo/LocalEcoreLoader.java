package com.sysml.platform.infrastructure.cdo;

import java.net.URL;
import java.util.List;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

/**
 * Loads local Ecore packages from classpath resources and registers them into the global EPackage
 * registry.
 */
public final class LocalEcoreLoader {

  private LocalEcoreLoader() {}

  public static void registerLocalEcorePackagesOrThrow(List<String> classpathEcorePaths) {
    ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("ecore", new EcoreResourceFactoryImpl());

    for (String path : classpathEcorePaths) {
      URL url = LocalEcoreLoader.class.getClassLoader().getResource(path);
      if (url == null) {
        throw new IllegalStateException("Ecore not found on classpath: " + path);
      }
      Resource resource =
          resourceSet.getResource(org.eclipse.emf.common.util.URI.createURI(url.toString()), true);
      resource.getContents().stream()
          .filter(EPackage.class::isInstance)
          .map(EPackage.class::cast)
          .forEach(pkg -> EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg));
    }
  }
}
