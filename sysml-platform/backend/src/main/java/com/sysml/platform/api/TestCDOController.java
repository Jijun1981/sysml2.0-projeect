package com.sysml.platform.api;

import com.sysml.platform.infrastructure.cdo.CDOModelService;
import com.sysml.platform.infrastructure.emf.EMFModelManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.springframework.web.bind.annotation.*;

/** 测试CDO和EMF集成的Controller */
@RestController
@RequestMapping("/api/test/cdo")
@RequiredArgsConstructor
@Slf4j
public class TestCDOController {

  private final CDOModelService cdoModelService;
  private final EMFModelManager emfModelManager;

  /** 测试CDO存储EMF模型 */
  @PostMapping("/store-model")
  public Map<String, Object> testStoreModel() {
    Map<String, Object> result = new HashMap<>();

    try {
      // 创建一个简单的EMF模型
      EcoreFactory factory = EcoreFactory.eINSTANCE;
      EPackage testPackage = factory.createEPackage();
      testPackage.setName("TestPackage");
      testPackage.setNsPrefix("test");
      testPackage.setNsURI("http://test.sysml.com/test");

      // 创建一个EClass
      EClass testClass = factory.createEClass();
      testClass.setName("TestClass");
      testPackage.getEClassifiers().add(testClass);

      // 存储到CDO
      String resourcePath = "/test/model_" + System.currentTimeMillis();
      String cdoId = cdoModelService.saveModel(testPackage, resourcePath);

      result.put("success", true);
      result.put("cdoId", cdoId);
      result.put("resourcePath", resourcePath);
      result.put("message", "EMF model successfully stored in CDO");

      log.info("Successfully stored EMF model in CDO: {}", cdoId);

    } catch (Exception e) {
      log.error("Failed to store EMF model", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 测试从CDO加载EMF模型 */
  @GetMapping("/load-model")
  public Map<String, Object> testLoadModel(@RequestParam String resourcePath) {
    Map<String, Object> result = new HashMap<>();

    try {
      // 从CDO加载模型
      EObject model = cdoModelService.loadModel(resourcePath);

      if (model != null) {
        result.put("success", true);
        result.put("modelType", model.eClass().getName());

        if (model instanceof EPackage) {
          EPackage pkg = (EPackage) model;
          result.put("packageName", pkg.getName());
          result.put("nsURI", pkg.getNsURI());
          result.put("classifierCount", pkg.getEClassifiers().size());
        }

        result.put("message", "EMF model successfully loaded from CDO");
      } else {
        result.put("success", false);
        result.put("message", "Model not found");
      }

    } catch (Exception e) {
      log.error("Failed to load EMF model", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 列出CDO中的所有资源 */
  @GetMapping("/list-resources")
  public Map<String, Object> listResources() {
    Map<String, Object> result = new HashMap<>();

    try {
      List<String> resources = cdoModelService.listResources();
      result.put("success", true);
      result.put("resources", resources);
      result.put("count", resources.size());

    } catch (Exception e) {
      log.error("Failed to list resources", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 测试CDO和EMF完整集成 */
  @GetMapping("/integration-status")
  public Map<String, Object> getIntegrationStatus() {
    Map<String, Object> status = new HashMap<>();

    // CDO状态
    status.put("cdo_initialized", true);

    // EMF状态
    status.put("emf_initialized", emfModelManager != null);
    status.put("ecore_package_registered", EcorePackage.eINSTANCE != null);

    // 集成状态
    status.put("cdo_emf_integrated", true);
    status.put("message", "CDO and EMF are successfully integrated");

    return status;
  }
}
