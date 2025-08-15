package com.sysml.platform.infrastructure.emf;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
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
@Slf4j
public class EMFModelManager {

  private ResourceSet resourceSet;
  private Resource localResource;
  private Map<String, EObject> objectCache = new HashMap<>();

  @PostConstruct
  public void initialize() {
    resourceSet = new ResourceSetImpl();
    resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());

    // 创建本地资源
    localResource = resourceSet.createResource(URI.createURI("memory://local.xmi"));
    log.info("EMF Model Manager initialized with local storage");
  }

  /** 创建EObject实例 */
  public EObject createObject(EClass eClass) {
    EObject object = EcoreUtil.create(eClass);
    log.debug("Created object of type: {}", eClass.getName());
    return object;
  }

  /** 查找对象 */
  public EObject findObject(String id) {
    // 先查缓存
    if (objectCache.containsKey(id)) {
      return objectCache.get(id);
    }

    // 查找本地资源
    for (EObject obj : localResource.getContents()) {
      String objId = EcoreUtil.getURI(obj).toString();
      if (objId.equals(id)) {
        objectCache.put(id, obj);
        return obj;
      }
    }

    return null;
  }

  /** 保存对象 */
  public String saveObject(EObject object) {
    // 本地模式 - 添加到ResourceSet
    if (!localResource.getContents().contains(object)) {
      localResource.getContents().add(object);
    }
    String id = EcoreUtil.getURI(object).toString();
    objectCache.put(id, object);
    log.info("Saved object locally with ID: {}", id);
    return id;
  }

  /** 更新对象 */
  public void updateObject(String id, Map<String, Object> updates) {
    EObject object = findObject(id);
    if (object == null) {
      throw new IllegalArgumentException("Object not found: " + id);
    }

    for (Map.Entry<String, Object> entry : updates.entrySet()) {
      EStructuralFeature feature = object.eClass().getEStructuralFeature(entry.getKey());
      if (feature != null) {
        object.eSet(feature, entry.getValue());
        log.debug("Updated feature {} = {}", entry.getKey(), entry.getValue());
      }
    }
  }

  /** 删除对象 */
  public void deleteObject(String id) {
    EObject object = findObject(id);
    if (object != null) {
      EcoreUtil.delete(object);
      objectCache.remove(id);
      log.info("Deleted object: {}", id);
    }
  }

  /** 序列化到XMI */
  public String serializeToXMI(EObject object) throws IOException {
    Resource tempResource = resourceSet.createResource(URI.createURI("temp://serialize.xmi"));
    tempResource.getContents().add(EcoreUtil.copy(object));

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    tempResource.save(baos, null);
    String xmi = baos.toString("UTF-8");

    resourceSet.getResources().remove(tempResource);
    return xmi;
  }

  /** 从XMI反序列化 */
  public EObject deserializeFromXMI(String xmi) throws IOException {
    Resource tempResource = resourceSet.createResource(URI.createURI("temp://deserialize.xmi"));
    ByteArrayInputStream bais = new ByteArrayInputStream(xmi.getBytes("UTF-8"));
    tempResource.load(bais, null);

    if (!tempResource.getContents().isEmpty()) {
      EObject object = tempResource.getContents().get(0);
      resourceSet.getResources().remove(tempResource);
      return object;
    }

    return null;
  }

  /** XMI往返验证 - 满足RQ-M2-ROUNDTRIP-003 */
  public boolean validateRoundTrip(EObject original) {
    try {
      // 序列化到XMI
      String xmi = serializeToXMI(original);
      log.debug("Serialized to XMI: {} bytes", xmi.length());

      // 反序列化
      EObject restored = deserializeFromXMI(xmi);

      // 深度比较
      boolean isEqual = EcoreUtil.equals(original, restored);
      log.info("Round-trip validation result: {}", isEqual ? "PASSED" : "FAILED");

      return isEqual;
    } catch (Exception e) {
      log.error("Round-trip validation failed", e);
      return false;
    }
  }

  /** 获取所有对象 */
  public List<EObject> getAllObjects() {
    List<EObject> allObjects = new ArrayList<>();
    allObjects.addAll(localResource.getContents());
    return allObjects;
  }

  /** 获取统计信息 */
  public Map<String, Object> getStatistics() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalObjects", localResource.getContents().size());
    stats.put("cachedObjects", objectCache.size());
    stats.put("mode", "LOCAL");
    stats.put("cdoEnabled", false);
    return stats;
  }
}
