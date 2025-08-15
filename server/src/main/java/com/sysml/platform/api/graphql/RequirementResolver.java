package com.sysml.platform.api.graphql;

import com.sysml.platform.domain.requirements.InMemoryRequirementService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.EObject;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL Resolver for Requirements 实现 P1 需求域的 GraphQL API 使用 EMF 动态模型，不依赖外部 JAR */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RequirementResolver {

  private final InMemoryRequirementService requirementService;

  // ========== Queries ==========

  /** 查询所有需求 */
  @QueryMapping
  public List<Map<String, Object>> requirements() {
    log.debug("Querying all requirements");

    return requirementService.findAllRequirements().stream()
        .map(this::toGraphQLMap)
        .collect(Collectors.toList());
  }

  /** 根据 reqId 查询需求 */
  @QueryMapping
  public Map<String, Object> requirement(@Argument String reqId) {
    log.debug("Querying requirement by reqId: {}", reqId);

    return requirementService.findByReqId(reqId).map(this::toGraphQLMap).orElse(null);
  }

  // ========== Mutations ==========

  /** 创建需求定义 */
  @MutationMapping
  public Map<String, Object> createRequirement(@Argument Map<String, String> input) {
    String reqId = input.get("reqId");
    String name = input.get("name");
    String text = input.get("text");

    log.info("Creating requirement: {}", reqId);

    EObject req = requirementService.createRequirementDefinition(reqId, name, text);

    return toGraphQLMap(req);
  }

  /** 添加 derive 关系 */
  @MutationMapping
  public Map<String, Object> addDeriveRelationship(
      @Argument String childId, @Argument String parentId) {
    log.info("Adding derive relationship: {} -> {}", childId, parentId);

    var child =
        requirementService
            .findByReqId(childId)
            .orElseThrow(() -> new RuntimeException("Child requirement not found: " + childId));
    var parent =
        requirementService
            .findByReqId(parentId)
            .orElseThrow(() -> new RuntimeException("Parent requirement not found: " + parentId));

    requirementService.addDeriveRelationship(child, parent);

    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("message", String.format("%s now derives from %s", childId, parentId));
    return result;
  }

  /** 添加 refine 关系（暂时使用 derive 实现） */
  @MutationMapping
  public Map<String, Object> addRefineRelationship(
      @Argument String refiningId, @Argument String refinedId) {
    log.info("Adding refine relationship: {} refines {}", refiningId, refinedId);

    // TODO: 实现真正的 refine 关系
    // 暂时使用 derive 关系代替
    return addDeriveRelationship(refiningId, refinedId);
  }

  // ========== Helper Methods ==========

  /** 转换 EObject 为 GraphQL Map */
  private Map<String, Object> toGraphQLMap(EObject req) {
    Map<String, Object> map = new HashMap<>();

    // 获取 elementId
    var elementIdFeature = req.eClass().getEStructuralFeature("elementId");
    if (elementIdFeature != null) {
      Object elementId = req.eGet(elementIdFeature);
      map.put("id", elementId != null ? elementId.toString() : req.hashCode());
    } else {
      map.put("id", String.valueOf(req.hashCode()));
    }

    map.put("reqId", requirementService.getReqId(req));
    map.put("name", requirementService.getName(req));
    map.put("text", requirementService.getText(req));

    // 添加派生需求
    List<String> derivedIds =
        requirementService.getDerivedRequirements(req).stream()
            .map(r -> requirementService.getReqId(r))
            .collect(Collectors.toList());
    map.put("derivedRequirements", derivedIds);

    return map;
  }
}
