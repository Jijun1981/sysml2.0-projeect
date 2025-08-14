package com.sysml.platform.api;

import com.sysml.platform.infrastructure.cdo.CDOModelService;
import com.sysml.platform.infrastructure.cdo.CDOServerManager;
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
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "cdo", name = "enabled", havingValue = "true")
@RequestMapping("/api/test/cdo")
@RequiredArgsConstructor
@Slf4j
public class TestCDOController {

  private final CDOModelService cdoModelService;
  private final CDOServerManager cdoServerManager;
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
    status.put("cdo_initialized", cdoServerManager.isHealthy());
    status.put("cdo_server_info", cdoServerManager.getServerInfo());

    // EMF状态
    status.put("emf_initialized", emfModelManager != null);
    status.put("ecore_package_registered", EcorePackage.eINSTANCE != null);

    // 集成状态
    boolean integrated = cdoServerManager.isHealthy() && emfModelManager != null;
    status.put("cdo_emf_integrated", integrated);
    status.put("message", integrated ? 
      "CDO and EMF are successfully integrated" : 
      "CDO/EMF integration has issues");

    return status;
  }

  /** CDO详细诊断信息 */
  @GetMapping("/diagnostic")
  public Map<String, Object> getDiagnosticInfo() {
    Map<String, Object> result = new HashMap<>();
    
    try {
      result.put("server_info", cdoServerManager.getServerInfo());
      result.put("diagnostic_info", cdoServerManager.getDiagnosticInfo());
      result.put("health_status", cdoServerManager.isHealthy());
      result.put("timestamp", System.currentTimeMillis());
      
    } catch (Exception e) {
      log.error("Failed to get diagnostic info", e);
      result.put("error", e.getMessage());
    }
    
    return result;
  }

  /** 测试Repository连接和PackageRegistry */
  @GetMapping("/test-repository")
  public Map<String, Object> testRepository() {
    Map<String, Object> result = new HashMap<>();
    
    try {
      var repository = cdoServerManager.getRepository();
      if (repository == null) {
        result.put("success", false);
        result.put("error", "Repository is null");
        return result;
      }
      
      result.put("repository_state", repository.getState().toString());
      result.put("repository_uuid", repository.getUUID());
      result.put("repository_name", repository.getName());
      
      // 测试Session
      var session = cdoServerManager.getSession();
      if (session != null && !session.isClosed()) {
        result.put("session_available", true);
        result.put("session_id", session.getSessionID());
        
        // 测试PackageRegistry
        var packageRegistry = session.getPackageRegistry();
        if (packageRegistry != null) {
          result.put("package_registry_available", true);
          result.put("registered_packages_count", packageRegistry.size());
          
          // 测试EPackage注册
          try {
            packageRegistry.putEPackage(EcorePackage.eINSTANCE);
            result.put("epackage_registration_test", "SUCCESS");
          } catch (Exception e) {
            result.put("epackage_registration_test", "FAILED: " + e.getMessage());
          }
        } else {
          result.put("package_registry_available", false);
        }
      } else {
        result.put("session_available", false);
      }
      
      result.put("success", true);
      
    } catch (Exception e) {
      log.error("Repository test failed", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }
    
    return result;
  }
}
