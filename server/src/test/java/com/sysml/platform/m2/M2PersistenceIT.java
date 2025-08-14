package com.sysml.platform.m2;

import static org.assertj.core.api.Assertions.assertThat;

import com.sysml.platform.infrastructure.cdo.CDOModelService;
import com.sysml.platform.infrastructure.cdo.LocalEcoreLoader;
import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {"spring.profiles.active=prod"})
class M2PersistenceIT {

  @Autowired private CDOModelService cdoModelService;

  @Test
  void shouldPersistAndLoad_M2_DynamicEObject() {
    // 1) Load and register local SysML/KerML/Types Ecore packages
    LocalEcoreLoader.registerLocalEcorePackagesOrThrow(
        java.util.List.of(
            "ecore/sysml/SysML.ecore", "ecore/sysml/kerml.ecore", "ecore/sysml/types.ecore"));

    // 2) Pick a concrete, non-abstract EClass from any of the loaded packages
    EPackage[] candidatePkgs =
        new EPackage[] {
          EPackage.Registry.INSTANCE.getEPackage("https://www.omg.org/spec/SysML/20250201"),
          EPackage.Registry.INSTANCE.getEPackage("https://www.omg.org/spec/KerML/20250201"),
          EPackage.Registry.INSTANCE.getEPackage("http://www.omg.org/2014/Types")
        };

    EClass concrete = null;
    EFactory factory = null;
    for (EPackage pkg : candidatePkgs) {
      if (pkg == null) continue;
      for (EClassifier classifier : pkg.getEClassifiers()) {
        if (classifier instanceof EClass) {
          EClass ec = (EClass) classifier;
          if (!ec.isAbstract() && !ec.isInterface()) {
            concrete = ec;
            factory = pkg.getEFactoryInstance();
            break;
          }
        }
      }
      if (concrete != null) break;
    }

    assertThat(concrete)
        .withFailMessage("No concrete EClass found in loaded M2 packages")
        .isNotNull();
    assertThat(factory).isNotNull();

    // 3) Bootstrap CDO repository (ensure ONLINE and resource root)
    cdoModelService.createEmptyResource("/m2/test-resource");

    // 4) Register packages into CDO repo package registry
    for (EPackage pkg : candidatePkgs) {
      if (pkg != null) {
        cdoModelService.registerPackage(pkg);
      }
    }

    // 5) Create a dynamic instance and persist via CDO
    EObject eObject = factory.create(concrete);
    String id = cdoModelService.saveModel(eObject, "/m2/test-resource");
    assertThat(id).isNotBlank();

    // 6) Load back
    EObject loaded = cdoModelService.loadModel("/m2/test-resource");
    assertThat(loaded).isNotNull();
  }
}
