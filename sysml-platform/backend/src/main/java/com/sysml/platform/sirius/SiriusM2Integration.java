package com.sysml.platform.sirius;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.*;
import org.springframework.web.bind.annotation.*;

/**
 * Sirius与M2模型集成 展示如何在Sirius中使用SysML v2的M2元模型
 *
 * <p>复杂度分析： 1. 简单 - 直接使用EMF ECore API创建模型实例 2. 中等 - 需要理解元模型结构和约束 3. 复杂 - 需要处理模型验证、转换和持久化
 */
@RestController
@RequestMapping("/api/sirius/m2")
@Slf4j
public class SiriusM2Integration {

  // 已注册的M2模型包
  private final Map<String, EPackage> m2Packages = new HashMap<>();

  public SiriusM2Integration() {
    initializeM2Models();
  }

  /** 初始化M2模型 - 这些是从ecore文件加载的 */
  private void initializeM2Models() {
    // 这些包已经通过LocalEcoreLoader注册
    EPackage.Registry registry = EPackage.Registry.INSTANCE;

    // 检查已加载的KerML和SysML包
    registry
        .keySet()
        .forEach(
            uri -> {
              if (uri.contains("kerml") || uri.contains("sysml")) {
                EPackage pkg = registry.getEPackage(uri);
                m2Packages.put(uri, pkg);
                log.info("M2包已加载: {} - {}", uri, pkg.getName());
              }
            });
  }

  /** 获取M2模型信息 - 展示复杂度 */
  @GetMapping("/info")
  public Map<String, Object> getM2Info() {
    Map<String, Object> info = new HashMap<>();

    // 1. 简单部分 - 列出可用的元模型
    List<Map<String, String>> models = new ArrayList<>();
    m2Packages.forEach(
        (uri, pkg) -> {
          Map<String, String> model = new HashMap<>();
          model.put("uri", uri);
          model.put("name", pkg.getName());
          model.put("classCount", String.valueOf(pkg.getEClassifiers().size()));
          models.add(model);
        });
    info.put("availableModels", models);

    // 2. 中等复杂度 - 展示如何使用M2创建实例
    info.put("exampleUsage", createExampleUsage());

    // 3. 复杂度评估
    info.put(
        "complexity",
        Map.of(
            "basic", "使用EMF Factory创建实例 - 简单",
            "intermediate", "理解元模型约束和关系 - 中等",
            "advanced", "模型转换、验证和CDO持久化 - 复杂"));

    return info;
  }

  /** 使用M2模型创建SysML元素 - 展示实际使用 */
  @PostMapping("/create")
  public Map<String, Object> createWithM2(@RequestBody Map<String, String> request) {
    String metaclass = request.get("metaclass"); // 如: "Requirement", "Part"
    String name = request.get("name");

    Map<String, Object> result = new HashMap<>();

    try {
      // 步骤1: 找到对应的EClass（从M2模型）
      EClass eClass = findEClass(metaclass);
      if (eClass == null) {
        throw new IllegalArgumentException("Metaclass not found: " + metaclass);
      }

      // 步骤2: 使用Factory创建实例
      EFactory factory = eClass.getEPackage().getEFactoryInstance();
      EObject instance = factory.create(eClass);

      // 步骤3: 设置属性
      EAttribute nameAttr = findAttribute(eClass, "name");
      if (nameAttr != null) {
        instance.eSet(nameAttr, name);
      }

      // 步骤4: 应用SysML特定的语义
      applySysMLSemantics(instance, metaclass);

      result.put("success", true);
      result.put("instance", describeInstance(instance));
      result.put("complexity", "这个过程涉及M2元模型查找、实例化和属性设置");

      log.info("通过M2创建了实例: {} ({})", name, metaclass);

    } catch (Exception e) {
      result.put("success", false);
      result.put("error", e.getMessage());
      log.error("创建失败", e);
    }

    return result;
  }

  /** 展示Sirius如何使用M2进行图形化建模 */
  @GetMapping("/sirius-usage")
  public Map<String, Object> showSiriusUsage() {
    Map<String, Object> usage = new HashMap<>();

    // Sirius使用M2的方式
    usage.put(
        "viewDefinition",
        Map.of(
            "description", "Sirius使用.odesign文件定义视图",
            "m2Reference", "视图定义引用M2元类",
            "example", "NodeMapping -> domainClass='sysml::Requirement'"));

    usage.put(
        "toolDefinition",
        Map.of(
            "description",
            "工具通过M2创建元素",
            "process",
            Arrays.asList("1. 用户点击工具", "2. Sirius查找M2元类", "3. 使用EFactory创建实例", "4. 应用工具定义的初始化")));

    usage.put(
        "validation",
        Map.of("description", "基于M2约束的验证", "examples", Arrays.asList("多重性检查", "类型兼容性", "必填属性")));

    usage.put("complexityLevel", "中等 - 需要理解EMF和元建模概念");

    return usage;
  }

  private EClass findEClass(String className) {
    for (EPackage pkg : m2Packages.values()) {
      for (EClassifier classifier : pkg.getEClassifiers()) {
        if (classifier instanceof EClass && classifier.getName().equalsIgnoreCase(className)) {
          return (EClass) classifier;
        }
      }
    }
    return null;
  }

  private EAttribute findAttribute(EClass eClass, String attrName) {
    return eClass.getEAllAttributes().stream()
        .filter(attr -> attr.getName().equals(attrName))
        .findFirst()
        .orElse(null);
  }

  private void applySysMLSemantics(EObject instance, String metaclass) {
    // 应用SysML特定的语义规则
    switch (metaclass.toLowerCase()) {
      case "requirement":
        // 设置需求特定属性
        setIfExists(instance, "id", "REQ-" + System.currentTimeMillis());
        setIfExists(instance, "status", "Draft");
        break;
      case "part":
        // 设置部件特定属性
        setIfExists(instance, "isComposite", true);
        break;
      case "port":
        // 设置端口特定属性
        setIfExists(instance, "direction", "inout");
        break;
    }
  }

  private void setIfExists(EObject obj, String featureName, Object value) {
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if (feature != null) {
      obj.eSet(feature, value);
    }
  }

  private Map<String, Object> describeInstance(EObject instance) {
    Map<String, Object> description = new HashMap<>();
    description.put("class", instance.eClass().getName());
    description.put("package", instance.eClass().getEPackage().getName());

    Map<String, Object> attributes = new HashMap<>();
    instance
        .eClass()
        .getEAllAttributes()
        .forEach(
            attr -> {
              Object value = instance.eGet(attr);
              if (value != null) {
                attributes.put(attr.getName(), value.toString());
              }
            });
    description.put("attributes", attributes);

    return description;
  }

  private Map<String, String> createExampleUsage() {
    Map<String, String> example = new HashMap<>();
    example.put("step1", "获取M2元类: EClass reqClass = getEClass('Requirement')");
    example.put("step2", "创建实例: EObject req = factory.create(reqClass)");
    example.put("step3", "设置属性: req.eSet(nameAttr, 'Safety Requirement')");
    example.put("step4", "保存到CDO: cdoResource.getContents().add(req)");
    return example;
  }
}
