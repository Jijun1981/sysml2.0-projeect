package com.sysml.platform.domain.requirements;

import com.sysml.platform.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.springframework.stereotype.Service;

/** 内存版需求服务 - 用于测试和演示 不依赖CDO，直接在内存中管理需求 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InMemoryRequirementService {

  // 内存存储
  private final Map<String, EObject> requirementStore = new HashMap<>();

  // SysML 包的 namespace URI
  private static final String SYSML_NS_URI = "https://www.omg.org/spec/SysML/20250201";

  /** 获取 SysML EPackage */
  private EPackage getSysMLPackage() {
    EPackage pkg = EPackage.Registry.INSTANCE.getEPackage(SYSML_NS_URI);
    if (pkg == null) {
      throw new BusinessException(
          "SYSML_NOT_LOADED", "SysML EPackage not found. Ensure ecore files are loaded.");
    }
    return pkg;
  }

  /** 创建需求定义 */
  public EObject createRequirementDefinition(String reqId, String name, String text) {
    log.debug("Creating RequirementDefinition with reqId: {}", reqId);

    // 检查 reqId 唯一性
    if (requirementStore.containsKey(reqId)) {
      throw new BusinessException("REQ_ID_DUPLICATE", "Requirement ID already exists: " + reqId);
    }

    EPackage sysmlPackage = getSysMLPackage();

    // 获取 RequirementDefinition EClass
    EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
    if (reqDefClass == null) {
      throw new BusinessException(
          "CLASS_NOT_FOUND", "RequirementDefinition class not found in SysML package");
    }

    // 使用 EFactory 创建实例
    EObject reqDef = sysmlPackage.getEFactoryInstance().create(reqDefClass);

    // 设置 elementId
    EStructuralFeature elementIdFeature = reqDefClass.getEStructuralFeature("elementId");
    if (elementIdFeature != null) {
      reqDef.eSet(elementIdFeature, UUID.randomUUID().toString());
    }

    // 设置 declaredShortName (reqId)
    EStructuralFeature shortNameFeature = reqDefClass.getEStructuralFeature("declaredShortName");
    if (shortNameFeature != null) {
      reqDef.eSet(shortNameFeature, reqId);
    }

    // 设置 declaredName
    EStructuralFeature nameFeature = reqDefClass.getEStructuralFeature("declaredName");
    if (nameFeature != null) {
      reqDef.eSet(nameFeature, name);
    }

    // 创建 Documentation 来存储需求文本
    if (text != null && !text.isEmpty()) {
      EClass docClass = (EClass) sysmlPackage.getEClassifier("Documentation");
      if (docClass != null) {
        EObject documentation = sysmlPackage.getEFactoryInstance().create(docClass);

        // 设置 body 属性
        EStructuralFeature bodyFeature = docClass.getEStructuralFeature("body");
        if (bodyFeature != null) {
          documentation.eSet(bodyFeature, text);
        }

        // 添加到 documentation 集合
        EStructuralFeature docFeature = reqDefClass.getEStructuralFeature("documentation");
        if (docFeature != null && docFeature instanceof EReference) {
          @SuppressWarnings("unchecked")
          List<EObject> docs = (List<EObject>) reqDef.eGet(docFeature);
          if (docs == null) {
            docs = new ArrayList<>();
            reqDef.eSet(docFeature, docs);
          }
          docs.add(documentation);
        }
      }
    }

    // 保存到内存
    requirementStore.put(reqId, reqDef);

    log.info("Created RequirementDefinition: {}", reqId);

    return reqDef;
  }

  /** 建立 derive 关系（派生） */
  public void addDeriveRelationship(EObject child, EObject parent) {
    String childId = getReqId(child);
    String parentId = getReqId(parent);

    log.debug("Adding derive relationship: {} -> {}", childId, parentId);

    EPackage sysmlPackage = getSysMLPackage();

    // 创建 Specialization 关系
    EClass specClass = (EClass) sysmlPackage.getEClassifier("Specialization");
    if (specClass != null) {
      EObject specialization = sysmlPackage.getEFactoryInstance().create(specClass);

      // 设置 general (父需求)
      EStructuralFeature generalFeature = specClass.getEStructuralFeature("general");
      if (generalFeature != null) {
        specialization.eSet(generalFeature, parent);
      }

      // 设置 specific (子需求)
      EStructuralFeature specificFeature = specClass.getEStructuralFeature("specific");
      if (specificFeature != null) {
        specialization.eSet(specificFeature, child);
      }

      // 添加到子需求的 ownedRelationship
      EStructuralFeature relFeature = child.eClass().getEStructuralFeature("ownedRelationship");
      if (relFeature != null && relFeature instanceof EReference) {
        @SuppressWarnings("unchecked")
        List<EObject> relationships = (List<EObject>) child.eGet(relFeature);
        if (relationships == null) {
          relationships = new ArrayList<>();
          child.eSet(relFeature, relationships);
        }
        relationships.add(specialization);
      }
    }

    log.info("Added derive relationship: {} derives from {}", childId, parentId);
  }

  /** 根据 reqId 查找需求 */
  public Optional<EObject> findByReqId(String reqId) {
    return Optional.ofNullable(requirementStore.get(reqId));
  }

  /** 查询所有需求 */
  public List<EObject> findAllRequirements() {
    return new ArrayList<>(requirementStore.values());
  }

  /** 获取需求的 reqId */
  public String getReqId(EObject req) {
    EStructuralFeature shortNameFeature = req.eClass().getEStructuralFeature("declaredShortName");
    if (shortNameFeature != null) {
      Object value = req.eGet(shortNameFeature);
      if (value != null) {
        return value.toString();
      }
    }
    return "REQ-UNKNOWN";
  }

  /** 获取需求的名称 */
  public String getName(EObject req) {
    EStructuralFeature nameFeature = req.eClass().getEStructuralFeature("declaredName");
    if (nameFeature != null) {
      Object value = req.eGet(nameFeature);
      if (value != null) {
        return value.toString();
      }
    }
    return "Unnamed";
  }

  /** 获取需求的文本 */
  public String getText(EObject req) {
    EStructuralFeature docFeature = req.eClass().getEStructuralFeature("documentation");
    if (docFeature != null && docFeature instanceof EReference) {
      @SuppressWarnings("unchecked")
      List<EObject> docs = (List<EObject>) req.eGet(docFeature);
      if (docs != null && !docs.isEmpty()) {
        EObject doc = docs.get(0);
        EStructuralFeature bodyFeature = doc.eClass().getEStructuralFeature("body");
        if (bodyFeature != null) {
          Object body = doc.eGet(bodyFeature);
          if (body != null) {
            return body.toString();
          }
        }
      }
    }
    return "";
  }

  /** 获取派生的需求 */
  public List<EObject> getDerivedRequirements(EObject parent) {
    List<EObject> derived = new ArrayList<>();

    findAllRequirements()
        .forEach(
            req -> {
              if (hasDeriveRelationTo(req, parent)) {
                derived.add(req);
              }
            });

    return derived;
  }

  /** 检查是否有 derive 关系 */
  private boolean hasDeriveRelationTo(EObject child, EObject parent) {
    EStructuralFeature relFeature = child.eClass().getEStructuralFeature("ownedRelationship");
    if (relFeature != null && relFeature instanceof EReference) {
      @SuppressWarnings("unchecked")
      List<EObject> relationships = (List<EObject>) child.eGet(relFeature);

      if (relationships != null) {
        return relationships.stream()
            .filter(rel -> "Specialization".equals(rel.eClass().getName()))
            .anyMatch(
                spec -> {
                  EStructuralFeature generalFeature =
                      spec.eClass().getEStructuralFeature("general");
                  if (generalFeature != null) {
                    return parent.equals(spec.eGet(generalFeature));
                  }
                  return false;
                });
      }
    }
    return false;
  }
}
