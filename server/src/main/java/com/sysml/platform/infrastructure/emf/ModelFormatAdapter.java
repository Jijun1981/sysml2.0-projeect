package com.sysml.platform.infrastructure.emf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.springframework.stereotype.Component;

/** 模型格式适配器 - XMI/JSON往返转换 满足RQ-M2-ROUNDTRIP-003: XMI往返等价，JSON往返等价 */
@Component
@Slf4j
public class ModelFormatAdapter {

  private final ResourceSet resourceSet;
  private final ObjectMapper objectMapper;

  public ModelFormatAdapter() {
    this.resourceSet = new ResourceSetImpl();
    this.resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());
    this.objectMapper = new ObjectMapper();
  }

  // ========== XMI 转换 ==========

  /** EObject转XMI字符串 */
  public String toXMI(EObject eObject) throws IOException {
    Resource resource = resourceSet.createResource(URI.createURI("temp://model.xmi"));
    resource.getContents().add(EcoreUtil.copy(eObject)); // 使用副本避免影响原对象

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Map<String, Object> options = new HashMap<>();
    options.put(
        Resource.OPTION_SAVE_ONLY_IF_CHANGED, Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
    resource.save(outputStream, options);

    String xmi = outputStream.toString("UTF-8");
    resource.getContents().clear();
    resourceSet.getResources().remove(resource);

    log.debug("Converted EObject to XMI ({} bytes)", xmi.length());
    return xmi;
  }

  /** XMI字符串转EObject */
  public EObject fromXMI(String xmi) throws IOException {
    Resource resource = resourceSet.createResource(URI.createURI("temp://model.xmi"));

    ByteArrayInputStream inputStream = new ByteArrayInputStream(xmi.getBytes("UTF-8"));
    resource.load(inputStream, null);

    if (resource.getContents().isEmpty()) {
      throw new IOException("No EObject found in XMI");
    }

    EObject result = resource.getContents().get(0);
    resource.getContents().clear();
    resourceSet.getResources().remove(resource);

    log.debug("Parsed EObject from XMI");
    return result;
  }

  // ========== JSON 转换 ==========

  /** EObject转JSON */
  public String toJSON(EObject eObject) throws IOException {
    Map<String, Object> json = eObjectToMap(eObject);
    String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    log.debug("Converted EObject to JSON ({} bytes)", jsonString.length());
    return jsonString;
  }

  /** JSON转EObject */
  @SuppressWarnings("unchecked")
  public EObject fromJSON(String json, EPackage ePackage) throws IOException {
    Map<String, Object> map = objectMapper.readValue(json, Map.class);
    EObject result = mapToEObject(map, ePackage);
    log.debug("Parsed EObject from JSON");
    return result;
  }

  /** EObject转Map（递归） */
  private Map<String, Object> eObjectToMap(EObject eObject) {
    Map<String, Object> map = new LinkedHashMap<>();

    // 元信息
    EClass eClass = eObject.eClass();
    map.put("_eClass", eClass.getName());
    map.put("_nsURI", eClass.getEPackage().getNsURI());

    // 属性
    for (EAttribute attr : eClass.getEAllAttributes()) {
      Object value = eObject.eGet(attr);
      if (value != null) {
        if (attr.isMany()) {
          @SuppressWarnings("unchecked")
          List<?> list = (List<?>) value;
          map.put(attr.getName(), new ArrayList<>(list));
        } else {
          map.put(attr.getName(), value.toString());
        }
      }
    }

    // 引用
    for (EReference ref : eClass.getEAllReferences()) {
      if (!ref.isDerived() && !ref.isTransient()) {
        Object value = eObject.eGet(ref);
        if (value != null) {
          if (ref.isMany()) {
            @SuppressWarnings("unchecked")
            List<EObject> list = (List<EObject>) value;
            List<Map<String, Object>> refList =
                list.stream().map(this::eObjectToMap).collect(Collectors.toList());
            map.put(ref.getName(), refList);
          } else {
            map.put(ref.getName(), eObjectToMap((EObject) value));
          }
        }
      }
    }

    return map;
  }

  /** Map转EObject（递归） */
  @SuppressWarnings("unchecked")
  private EObject mapToEObject(Map<String, Object> map, EPackage rootPackage) {
    String eClassName = (String) map.get("_eClass");
    String nsURI = (String) map.get("_nsURI");

    // 查找EPackage
    EPackage ePackage = rootPackage;
    if (nsURI != null && !nsURI.equals(rootPackage.getNsURI())) {
      ePackage = EPackage.Registry.INSTANCE.getEPackage(nsURI);
      if (ePackage == null) {
        ePackage = rootPackage; // fallback
      }
    }

    // 查找EClass
    EClassifier classifier = ePackage.getEClassifier(eClassName);
    if (!(classifier instanceof EClass)) {
      throw new IllegalArgumentException("EClass not found: " + eClassName);
    }

    EClass eClass = (EClass) classifier;
    EObject eObject = EcoreUtil.create(eClass);

    // 设置属性和引用
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("_")) continue; // 跳过元信息

      EStructuralFeature feature = eClass.getEStructuralFeature(key);
      if (feature == null) continue;

      Object value = entry.getValue();
      if (value == null) continue;

      if (feature instanceof EAttribute) {
        EAttribute attr = (EAttribute) feature;
        if (attr.isMany()) {
          List<?> list = (List<?>) value;
          ((List<Object>) eObject.eGet(attr)).addAll(list);
        } else {
          // 转换字符串到正确的类型
          Object convertedValue = convertValue(value.toString(), attr.getEAttributeType());
          eObject.eSet(attr, convertedValue);
        }
      } else if (feature instanceof EReference) {
        EReference ref = (EReference) feature;
        if (ref.isMany()) {
          List<Map<String, Object>> list = (List<Map<String, Object>>) value;
          final EPackage finalPackage = ePackage;
          List<EObject> refObjects =
              list.stream().map(m -> mapToEObject(m, finalPackage)).collect(Collectors.toList());
          ((List<EObject>) eObject.eGet(ref)).addAll(refObjects);
        } else {
          Map<String, Object> refMap = (Map<String, Object>) value;
          EObject refObject = mapToEObject(refMap, ePackage);
          eObject.eSet(ref, refObject);
        }
      }
    }

    return eObject;
  }

  /** 值类型转换 */
  private Object convertValue(String value, EDataType dataType) {
    if (dataType == EcorePackage.Literals.ESTRING) {
      return value;
    } else if (dataType == EcorePackage.Literals.EINT) {
      return Integer.parseInt(value);
    } else if (dataType == EcorePackage.Literals.EBOOLEAN) {
      return Boolean.parseBoolean(value);
    } else if (dataType == EcorePackage.Literals.EDOUBLE) {
      return Double.parseDouble(value);
    } else if (dataType == EcorePackage.Literals.EFLOAT) {
      return Float.parseFloat(value);
    } else if (dataType == EcorePackage.Literals.ELONG) {
      return Long.parseLong(value);
    } else {
      // 使用工厂方法
      return EcoreUtil.createFromString(dataType, value);
    }
  }

  // ========== 往返验证 ==========

  /** XMI往返验证 */
  public boolean validateXMIRoundTrip(EObject original) {
    try {
      // 第一次转换
      String xmi1 = toXMI(original);

      // 反序列化
      EObject restored = fromXMI(xmi1);

      // 第二次转换
      String xmi2 = toXMI(restored);

      // 比较XMI字符串
      boolean xmiEqual = xmi1.equals(xmi2);

      // 比较对象
      boolean objectEqual = EcoreUtil.equals(original, restored);

      boolean passed = xmiEqual && objectEqual;

      log.info(
          "XMI round-trip validation: {} (XMI equal: {}, Object equal: {})",
          passed ? "PASSED" : "FAILED",
          xmiEqual,
          objectEqual);

      if (!passed && log.isDebugEnabled()) {
        log.debug("Original XMI length: {}", xmi1.length());
        log.debug("Restored XMI length: {}", xmi2.length());
      }

      return passed;

    } catch (Exception e) {
      log.error("XMI round-trip validation failed", e);
      return false;
    }
  }

  /** JSON往返验证 */
  public boolean validateJSONRoundTrip(EObject original) {
    try {
      EPackage ePackage = original.eClass().getEPackage();

      // 第一次转换
      String json1 = toJSON(original);

      // 反序列化
      EObject restored = fromJSON(json1, ePackage);

      // 第二次转换
      String json2 = toJSON(restored);

      // 比较JSON字符串
      boolean jsonEqual = json1.equals(json2);

      // 比较对象
      boolean objectEqual = EcoreUtil.equals(original, restored);

      boolean passed = jsonEqual && objectEqual;

      log.info(
          "JSON round-trip validation: {} (JSON equal: {}, Object equal: {})",
          passed ? "PASSED" : "FAILED",
          jsonEqual,
          objectEqual);

      if (!passed && log.isDebugEnabled()) {
        log.debug("Original JSON length: {}", json1.length());
        log.debug("Restored JSON length: {}", json2.length());
      }

      return passed;

    } catch (Exception e) {
      log.error("JSON round-trip validation failed", e);
      return false;
    }
  }

  /** 批量往返验证 */
  public Map<String, Boolean> validateBatchRoundTrip(List<EObject> objects) {
    Map<String, Boolean> results = new HashMap<>();

    for (int i = 0; i < objects.size(); i++) {
      EObject obj = objects.get(i);
      String key = "Object_" + i + "_" + obj.eClass().getName();

      boolean xmiPassed = validateXMIRoundTrip(obj);
      boolean jsonPassed = validateJSONRoundTrip(obj);

      results.put(key + "_XMI", xmiPassed);
      results.put(key + "_JSON", jsonPassed);
    }

    long xmiPassed =
        results.entrySet().stream()
            .filter(e -> e.getKey().endsWith("_XMI"))
            .filter(Map.Entry::getValue)
            .count();

    long jsonPassed =
        results.entrySet().stream()
            .filter(e -> e.getKey().endsWith("_JSON"))
            .filter(Map.Entry::getValue)
            .count();

    log.info(
        "Batch validation complete: XMI {}/{}, JSON {}/{}",
        xmiPassed,
        objects.size(),
        jsonPassed,
        objects.size());

    return results;
  }
}
