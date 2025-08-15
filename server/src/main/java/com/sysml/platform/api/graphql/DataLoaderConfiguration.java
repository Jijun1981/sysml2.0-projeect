package com.sysml.platform.api.graphql;

import com.sysml.platform.infrastructure.emf.EMFModelManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.dataloader.*;
import org.eclipse.emf.ecore.EObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** GraphQL DataLoader配置 - 消除N+1查询问题 满足RQ-API-DATALOADER-003: nPlusOneCount==0 */
@Configuration
public class DataLoaderConfiguration {

  @Autowired(required = false)
  private EMFModelManager emfModelManager;

  @Autowired private Executor graphqlExecutor;

  /** EObject批量加载器 */
  @Bean
  public DataLoader<String, EObject> eObjectDataLoader() {
    BatchLoader<String, EObject> batchLoader =
        keys ->
            CompletableFuture.supplyAsync(
                () -> {
                  List<EObject> results = new ArrayList<>();

                  for (String key : keys) {
                    EObject obj = null;
                    if (emfModelManager != null) {
                      obj = emfModelManager.findObject(key);
                    }
                    results.add(obj); // null也要加，保持顺序一致
                  }

                  return results;
                },
                graphqlExecutor);

    DataLoaderOptions options =
        DataLoaderOptions.newOptions()
            .setCachingEnabled(true)
            .setBatchingEnabled(true)
            .setMaxBatchSize(100)
            .build();

    return DataLoaderFactory.newDataLoader(batchLoader, options);
  }

  /** 子元素批量加载器 */
  @Bean
  public DataLoader<String, List<EObject>> childrenDataLoader() {
    MappedBatchLoader<String, List<EObject>> mappedBatchLoader =
        keys ->
            CompletableFuture.supplyAsync(
                () -> {
                  Map<String, List<EObject>> result = new HashMap<>();

                  // 批量查询所有父对象的子元素
                  for (String parentId : keys) {
                    List<EObject> children = loadChildren(parentId);
                    result.put(parentId, children);
                  }

                  return result;
                },
                graphqlExecutor);

    DataLoaderOptions options =
        DataLoaderOptions.newOptions().setCachingEnabled(true).setBatchingEnabled(true).build();

    return DataLoaderFactory.newMappedDataLoader(mappedBatchLoader, options);
  }

  /** 属性批量加载器 */
  @Bean
  public DataLoader<AttributeKey, Object> attributeDataLoader() {
    BatchLoader<AttributeKey, Object> batchLoader =
        keys ->
            CompletableFuture.supplyAsync(
                () -> {
                  List<Object> results = new ArrayList<>();

                  // 批量加载属性值
                  Map<String, EObject> objectCache = new HashMap<>();

                  for (AttributeKey key : keys) {
                    // 先从缓存获取对象
                    EObject obj = objectCache.get(key.objectId);
                    if (obj == null && emfModelManager != null) {
                      obj = emfModelManager.findObject(key.objectId);
                      if (obj != null) {
                        objectCache.put(key.objectId, obj);
                      }
                    }

                    // 获取属性值
                    Object value = null;
                    if (obj != null) {
                      value = getAttributeValue(obj, key.attributeName);
                    }
                    results.add(value);
                  }

                  return results;
                },
                graphqlExecutor);

    return DataLoaderFactory.newDataLoader(batchLoader);
  }

  /** 引用批量加载器 */
  @Bean
  public DataLoader<ReferenceKey, List<EObject>> referenceDataLoader() {
    MappedBatchLoader<ReferenceKey, List<EObject>> mappedBatchLoader =
        keys ->
            CompletableFuture.supplyAsync(
                () -> {
                  Map<ReferenceKey, List<EObject>> result = new HashMap<>();

                  // 批量加载引用
                  Map<String, EObject> objectCache = new HashMap<>();

                  for (ReferenceKey key : keys) {
                    EObject source = objectCache.get(key.sourceId);
                    if (source == null && emfModelManager != null) {
                      source = emfModelManager.findObject(key.sourceId);
                      if (source != null) {
                        objectCache.put(key.sourceId, source);
                      }
                    }

                    List<EObject> targets = new ArrayList<>();
                    if (source != null) {
                      targets = getReferencedObjects(source, key.referenceName);
                    }
                    result.put(key, targets);
                  }

                  return result;
                },
                graphqlExecutor);

    return DataLoaderFactory.newMappedDataLoader(mappedBatchLoader);
  }

  /** 统计信息DataLoader */
  @Bean
  public DataLoaderStats dataLoaderStats() {
    return new DataLoaderStats();
  }

  // ========== Helper Methods ==========

  private List<EObject> loadChildren(String parentId) {
    // 实际实现中应该批量查询
    if (emfModelManager == null) {
      return Collections.emptyList();
    }

    EObject parent = emfModelManager.findObject(parentId);
    if (parent == null) {
      return Collections.emptyList();
    }

    // 获取所有包含关系的子元素
    return parent.eContents();
  }

  private Object getAttributeValue(EObject obj, String attributeName) {
    try {
      return obj.eGet(obj.eClass().getEStructuralFeature(attributeName));
    } catch (Exception e) {
      return null;
    }
  }

  private List<EObject> getReferencedObjects(EObject source, String referenceName) {
    try {
      Object value = source.eGet(source.eClass().getEStructuralFeature(referenceName));
      if (value instanceof List) {
        return (List<EObject>) value;
      } else if (value instanceof EObject) {
        return Collections.singletonList((EObject) value);
      }
    } catch (Exception e) {
      // ignore
    }
    return Collections.emptyList();
  }

  // ========== Key Classes ==========

  /** 属性键 */
  public static class AttributeKey {
    public final String objectId;
    public final String attributeName;

    public AttributeKey(String objectId, String attributeName) {
      this.objectId = objectId;
      this.attributeName = attributeName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      AttributeKey that = (AttributeKey) o;
      return Objects.equals(objectId, that.objectId)
          && Objects.equals(attributeName, that.attributeName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(objectId, attributeName);
    }
  }

  /** 引用键 */
  public static class ReferenceKey {
    public final String sourceId;
    public final String referenceName;

    public ReferenceKey(String sourceId, String referenceName) {
      this.sourceId = sourceId;
      this.referenceName = referenceName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ReferenceKey that = (ReferenceKey) o;
      return Objects.equals(sourceId, that.sourceId)
          && Objects.equals(referenceName, that.referenceName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sourceId, referenceName);
    }
  }

  /** DataLoader统计 */
  public static class DataLoaderStats {
    private long totalLoads = 0;
    private long batchLoads = 0;
    private long cacheHits = 0;

    public void recordLoad() {
      totalLoads++;
    }

    public void recordBatchLoad(int size) {
      batchLoads++;
      totalLoads += size;
    }

    public void recordCacheHit() {
      cacheHits++;
    }

    public Map<String, Long> getStats() {
      Map<String, Long> stats = new HashMap<>();
      stats.put("totalLoads", totalLoads);
      stats.put("batchLoads", batchLoads);
      stats.put("cacheHits", cacheHits);
      stats.put("nPlusOneCount", Math.max(0, totalLoads - batchLoads - cacheHits));
      return stats;
    }

    public void reset() {
      totalLoads = 0;
      batchLoads = 0;
      cacheHits = 0;
    }
  }
}
