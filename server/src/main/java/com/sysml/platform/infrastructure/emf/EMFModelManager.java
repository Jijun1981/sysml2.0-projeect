package com.sysml.platform.infrastructure.emf;

import com.sysml.platform.infrastructure.cdo.CDORepository;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.springframework.stereotype.Service;

/** EMF模型管理器 - 实现完整的CRUD和往返验证 满足RQ-INFRA-EMF-003和RQ-M2-FACTORY-002需求 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EMFModelManager {

  private final CDORepository cdoRepository;
  private ResourceSet resourceSet;

  @PostConstruct
  public void initialize() {
    resourceSet = new ResourceSetImpl();
    // 注册XMI资源工厂
    resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());

    log.info("EMFModelManager initialized");
  }

  /** 创建模型对象 (使用工厂) */
  public EObject createObject(String nsUri, String className) {
    log.debug("Creating object: {}:{}", nsUri, className);

    // 获取EPackage
    EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(nsUri);
    if (ePackage == null) {
      throw new IllegalArgumentException("EPackage not found: " + nsUri);
    }

    // 获取EClass
    EClassifier eClassifier = ePackage.getEClassifier(className);
    if (!(eClassifier instanceof EClass)) {
      throw new IllegalArgumentException("EClass not found: " + className);
    }

    // 使用工厂创建实例
    EFactory factory = ePackage.getEFactoryInstance();
    EObject instance = factory.create((EClass) eClassifier);

    log.info("Created {} instance", className);
    return instance;
  }

  /** 保存对象到CDO */
  public String saveObject(EObject object) {
    var transaction = cdoRepository.openTransaction();
    try {
      transaction.addObject(object);
      transaction.commit();

      String id = EcoreUtil.getURI(object).toString();
      log.info("Saved object with ID: {}", id);
      return id;
    } catch (Exception e) {
      transaction.rollback();
      throw new RuntimeException("Failed to save object", e);
    } finally {
      transaction.close();
    }
  }

  /** 查询对象 */
  public EObject findObject(String uri) {
    URI emfUri = URI.createURI(uri);
    Resource resource = resourceSet.getResource(emfUri, false);
    if (resource != null && !resource.getContents().isEmpty()) {
      return resource.getContents().get(0);
    }
    return null;
  }

  /** 更新对象属性 */
  public void updateObject(EObject object, String featureName, Object value) {
    EClass eClass = object.eClass();
    EStructuralFeature feature = eClass.getEStructuralFeature(featureName);

    if (feature == null) {
      throw new IllegalArgumentException("Feature not found: " + featureName);
    }

    object.eSet(feature, value);
    log.debug("Updated {}.{} = {}", eClass.getName(), featureName, value);
  }

  /** 删除对象 */
  public boolean deleteObject(EObject object) {
    EcoreUtil.delete(object, true);
    log.info("Deleted object");
    return true;
  }

  /** XMI序列化 */
  public String toXMI(EObject object) throws IOException {
    Resource resource = resourceSet.createResource(URI.createURI("temp.xmi"));
    resource.getContents().add(object);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    resource.save(outputStream, null);

    String xmi = outputStream.toString("UTF-8");
    resource.getContents().clear();
    return xmi;
  }

  /** XMI反序列化 */
  public EObject fromXMI(String xmi) throws IOException {
    Resource resource = resourceSet.createResource(URI.createURI("temp.xmi"));

    ByteArrayInputStream inputStream = new ByteArrayInputStream(xmi.getBytes("UTF-8"));
    resource.load(inputStream, null);

    if (resource.getContents().isEmpty()) {
      throw new IOException("No object found in XMI");
    }

    EObject object = resource.getContents().get(0);
    resource.getContents().clear();
    return object;
  }

  /** JSON序列化 (简化版) */
  public Map<String, Object> toJSON(EObject object) {
    Map<String, Object> json = new HashMap<>();
    EClass eClass = object.eClass();

    json.put("eClass", eClass.getName());
    json.put("nsUri", eClass.getEPackage().getNsURI());

    for (EAttribute attr : eClass.getEAllAttributes()) {
      Object value = object.eGet(attr);
      if (value != null) {
        json.put(attr.getName(), value.toString());
      }
    }

    return json;
  }

  /** 往返验证 - XMI */
  public boolean validateXMIRoundTrip(EObject original) {
    try {
      // 序列化
      String xmi = toXMI(original);

      // 反序列化
      EObject restored = fromXMI(xmi);

      // 比较
      boolean equal = EcoreUtil.equals(original, restored);
      log.info("XMI round-trip validation: {}", equal ? "PASSED" : "FAILED");

      return equal;
    } catch (Exception e) {
      log.error("XMI round-trip validation failed", e);
      return false;
    }
  }

  /** 批量创建测试 */
  public List<EObject> createMultipleObjects(String nsUri, String className, int count) {
    List<EObject> objects = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      objects.add(createObject(nsUri, className));
    }
    log.info("Created {} {} instances", count, className);
    return objects;
  }
}
