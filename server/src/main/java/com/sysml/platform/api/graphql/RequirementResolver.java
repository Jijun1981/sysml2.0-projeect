package com.sysml.platform.api.graphql;

import com.sysml.platform.api.graphql.payload.*;
import com.sysml.platform.domain.requirements.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Requirements域的GraphQL Resolver 实现查询和变更操作 */
@Controller
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(value = "features.requirements", havingValue = "true", matchIfMissing = true)
public class RequirementResolver {

  private final RequirementService requirementService;

  // ========== 查询操作 ==========

  @QueryMapping
  public RequirementDefinition requirement(@Argument String id) {
    log.debug("Query requirement by id: {}", id);
    return requirementService.findById(id).orElse(null);
  }

  @QueryMapping
  public RequirementsPage requirements(@Argument Integer page, @Argument Integer size) {
    int pageNum = page != null ? page : 1;
    int pageSize = size != null ? size : 20;
    log.debug("Query requirements page: {}, size: {}", pageNum, pageSize);
    return requirementService.findAll(pageNum, pageSize);
  }

  // ========== 变更操作 ==========

  @MutationMapping
  public CreateRequirementPayload createRequirement(@Argument CreateRequirementInput input) {
    log.info("Creating requirement with reqId: {}", input.getReqId());
    try {
      RequirementDefinition requirement = requirementService.createRequirement(input);
      return CreateRequirementPayload.builder().ok(true).requirement(requirement).build();
    } catch (Exception e) {
      log.error("Failed to create requirement", e);
      return CreateRequirementPayload.builder().ok(false).error(buildError(e)).build();
    }
  }

  @MutationMapping
  public UpdateRequirementPayload updateRequirement(
      @Argument String id, @Argument UpdateRequirementInput input) {
    log.info("Updating requirement: {}", id);
    try {
      RequirementDefinition requirement = requirementService.updateRequirement(id, input);
      return UpdateRequirementPayload.builder().ok(true).requirement(requirement).build();
    } catch (Exception e) {
      log.error("Failed to update requirement", e);
      return UpdateRequirementPayload.builder().ok(false).error(buildError(e)).build();
    }
  }

  @MutationMapping
  public DeletePayload deleteRequirement(@Argument String id) {
    log.info("Deleting requirement: {}", id);
    try {
      boolean deleted = requirementService.deleteRequirement(id);
      return DeletePayload.builder().ok(deleted).deletedId(deleted ? id : null).build();
    } catch (Exception e) {
      log.error("Failed to delete requirement", e);
      return DeletePayload.builder().ok(false).error(buildError(e)).build();
    }
  }

  /** 构建错误响应 */
  private com.sysml.platform.api.graphql.payload.Error buildError(Exception e) {
    String code = "INTERNAL_ERROR";
    String messageKey = "error.internal";

    if (e instanceof com.sysml.platform.common.exception.BusinessException) {
      var be = (com.sysml.platform.common.exception.BusinessException) e;
      code = be.getCode();
      messageKey = be.getMessageKey();
    }

    return com.sysml.platform.api.graphql.payload.Error.builder()
        .code(code)
        .messageKey(messageKey)
        .details(e.getMessage())
        .build();
  }
}
