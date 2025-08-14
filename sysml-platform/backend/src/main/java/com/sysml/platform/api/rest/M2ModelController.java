package com.sysml.platform.api.rest;

import com.sysml.platform.infrastructure.m2.M2ModelRegistry;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/m2")
@CrossOrigin(origins = "*")
public class M2ModelController {

  @Autowired private M2ModelRegistry m2Registry;

  @GetMapping("/status")
  public Map<String, Object> getModelStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("loaded", m2Registry.isModelLoaded());
    status.put("statistics", m2Registry.getModelStatistics());
    status.put("timestamp", new Date());
    return status;
  }

  @GetMapping("/packages")
  public Map<String, Object> getPackages() {
    Map<String, Object> result = new HashMap<>();

    EPackage kerml = m2Registry.getKermlPackage();
    EPackage sysml = m2Registry.getSysmlPackage();

    if (kerml != null) {
      Map<String, Object> kermlInfo = new HashMap<>();
      kermlInfo.put("name", kerml.getName());
      kermlInfo.put("nsURI", kerml.getNsURI());
      kermlInfo.put("nsPrefix", kerml.getNsPrefix());
      kermlInfo.put("classCount", kerml.getEClassifiers().size());
      result.put("kerml", kermlInfo);
    }

    if (sysml != null) {
      Map<String, Object> sysmlInfo = new HashMap<>();
      sysmlInfo.put("name", sysml.getName());
      sysmlInfo.put("nsURI", sysml.getNsURI());
      sysmlInfo.put("nsPrefix", sysml.getNsPrefix());
      sysmlInfo.put("classCount", sysml.getEClassifiers().size());
      result.put("sysml", sysmlInfo);
    }

    return result;
  }

  @GetMapping("/classes/{packageName}")
  public List<Map<String, Object>> getClasses(@PathVariable String packageName) {
    EPackage pkg = null;

    if ("kerml".equalsIgnoreCase(packageName)) {
      pkg = m2Registry.getKermlPackage();
    } else if ("sysml".equalsIgnoreCase(packageName)) {
      pkg = m2Registry.getSysmlPackage();
    }

    if (pkg == null) {
      return Collections.emptyList();
    }

    return pkg.getEClassifiers().stream()
        .filter(c -> c instanceof EClass)
        .map(
            c -> {
              EClass eClass = (EClass) c;
              Map<String, Object> classInfo = new HashMap<>();
              classInfo.put("name", eClass.getName());
              classInfo.put("abstract", eClass.isAbstract());
              classInfo.put("interface", eClass.isInterface());
              classInfo.put("attributeCount", eClass.getEAttributes().size());
              classInfo.put("referenceCount", eClass.getEReferences().size());

              // 超类
              List<String> superTypes =
                  eClass.getESuperTypes().stream()
                      .map(EClass::getName)
                      .collect(Collectors.toList());
              classInfo.put("superTypes", superTypes);

              return classInfo;
            })
        .collect(Collectors.toList());
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    Map<String, String> health = new HashMap<>();
    if (m2Registry.isModelLoaded()) {
      health.put("status", "UP");
      health.put("message", "M2 models loaded successfully");
    } else {
      health.put("status", "DOWN");
      health.put("message", "M2 models not loaded");
    }
    return health;
  }
}
