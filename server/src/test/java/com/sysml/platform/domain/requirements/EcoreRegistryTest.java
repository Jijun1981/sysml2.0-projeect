package com.sysml.platform.domain.requirements;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.Test;

public class EcoreRegistryTest {
  @Test
  void sysmlAndKermlEcoreShouldBeRegistered() {
    // Ensure local Ecore resources are loaded for the test JVM
    ResourceSet rs = new ResourceSetImpl();
    rs.getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("ecore", new EcoreResourceFactoryImpl());
    load(rs, "ecore/sysml/SysML.ecore");
    load(rs, "ecore/sysml/kerml.ecore");

    assertNotNull(EPackage.Registry.INSTANCE.get("https://www.omg.org/spec/SysML/20250201"));
    assertNotNull(EPackage.Registry.INSTANCE.get("https://www.omg.org/spec/KerML/20250201"));
  }

  private static void load(ResourceSet rs, String cpPath) {
    var url = EcoreRegistryTest.class.getClassLoader().getResource(cpPath);
    assertNotNull(url, "classpath not found: " + cpPath);
    Resource r = rs.getResource(org.eclipse.emf.common.util.URI.createURI(url.toString()), true);
    assertFalse(r.getContents().isEmpty(), "ecore empty: " + cpPath);
    r.getContents().stream()
        .filter(EPackage.class::isInstance)
        .map(EPackage.class::cast)
        .forEach(pkg -> EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg));
  }
}
