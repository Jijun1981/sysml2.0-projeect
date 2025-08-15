package com.sysml.platform.domain.requirements;

import com.sysml.platform.common.exception.BusinessException;
import com.sysml.platform.domain.requirements.entity.RequirementEntity;
import com.sysml.platform.domain.requirements.repository.RequirementRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 需求域服务 - 实现完整的CRUD和层次管理 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    value = "features.requirements",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequirementService {

  private final RequirementRepository repository;

  /** 创建需求 */
  @Transactional
  public RequirementDefinition createRequirement(CreateRequirementInput input) {
    log.debug("Creating requirement with reqId: {}", input.getReqId());

    // 检查reqId唯一性
    if (repository.existsByReqId(input.getReqId())) {
      throw new BusinessException(
          "REQ_ID_DUPLICATE", "Requirement ID already exists: " + input.getReqId());
    }

    RequirementEntity entity = new RequirementEntity();
    entity.setReqId(input.getReqId());
    entity.setName(input.getName());
    entity.setText(input.getText());

    // 处理父需求关系
    if (input.getParentId() != null) {
      UUID parentUuid = UUID.fromString(input.getParentId());
      RequirementEntity parent =
          repository
              .findById(parentUuid)
              .orElseThrow(
                  () -> new BusinessException("REQ_NOT_FOUND", "Parent requirement not found"));

      // 检查是否会形成循环
      if (repository.wouldCreateCycle(entity.getId(), parentUuid)) {
        throw new BusinessException("REQ_CYCLE_DETECTED", "Setting parent would create a cycle");
      }

      entity.setParent(parent);
      entity.setHierarchyType(RequirementEntity.HierarchyType.DERIVE); // 默认为derive关系
    }

    entity = repository.save(entity);
    log.info("Created requirement: {} ({})", entity.getReqId(), entity.getId());

    return toDefinition(entity);
  }

  /** 根据ID查找需求 */
  public Optional<RequirementDefinition> findById(String id) {
    try {
      UUID uuid = UUID.fromString(id);
      return repository.findById(uuid).map(this::toDefinition);
    } catch (IllegalArgumentException e) {
      // 尝试按业务ID查找
      return repository.findByReqId(id).map(this::toDefinition);
    }
  }

  /** 分页查询所有需求 */
  public RequirementsPage findAll(int page, int size) {
    var pageable = PageRequest.of(page - 1, size); // 转换为0-based
    var pageResult = repository.findAll(pageable);

    var items =
        pageResult.getContent().stream().map(this::toDefinition).collect(Collectors.toList());

    return RequirementsPage.builder()
        .items(items)
        .pageInfo(
            RequirementsPage.PageInfo.builder()
                .total((int) pageResult.getTotalElements())
                .page(page)
                .size(size)
                .hasNext(pageResult.hasNext())
                .hasPrevious(pageResult.hasPrevious())
                .build())
        .build();
  }

  /** 更新需求 */
  @Transactional
  public RequirementDefinition updateRequirement(String id, UpdateRequirementInput input) {
    UUID uuid = UUID.fromString(id);
    RequirementEntity entity =
        repository
            .findById(uuid)
            .orElseThrow(() -> new BusinessException("REQ_NOT_FOUND", "Requirement not found"));

    // 更新可修改字段
    if (input.getName() != null) {
      entity.setName(input.getName());
    }
    if (input.getText() != null) {
      entity.setText(input.getText());
    }

    // 处理父需求变更
    if (input.getParentId() != null) {
      if ("null".equals(input.getParentId())) {
        // 移除父需求
        entity.setParent(null);
      } else {
        UUID newParentId = UUID.fromString(input.getParentId());
        if (!newParentId.equals(entity.getParent() != null ? entity.getParent().getId() : null)) {
          // 检查循环依赖
          if (repository.wouldCreateCycle(uuid, newParentId)) {
            throw new BusinessException("REQ_CYCLE_DETECTED", "New parent would create a cycle");
          }

          RequirementEntity newParent =
              repository
                  .findById(newParentId)
                  .orElseThrow(
                      () -> new BusinessException("REQ_NOT_FOUND", "New parent not found"));
          entity.setParent(newParent);
        }
      }
    }

    entity = repository.save(entity);
    log.info("Updated requirement: {}", entity.getReqId());

    return toDefinition(entity);
  }

  /** 删除需求 */
  @Transactional
  public boolean deleteRequirement(String id) {
    UUID uuid = UUID.fromString(id);

    if (!repository.existsById(uuid)) {
      return false;
    }

    // 检查是否有子需求
    var children = repository.findByParentId(uuid);
    if (!children.isEmpty()) {
      throw new BusinessException("REF_IN_USE", "Cannot delete requirement with children");
    }

    repository.deleteById(uuid);
    log.info("Deleted requirement: {}", id);

    return true;
  }

  /** 实体转换为DTO */
  private RequirementDefinition toDefinition(RequirementEntity entity) {
    return RequirementDefinition.builder()
        .id(entity.getId().toString())
        .reqId(entity.getReqId())
        .name(entity.getName())
        .text(entity.getText())
        .parent(entity.getParent() != null ? toDefinition(entity.getParent()) : null)
        .children(
            entity.getChildren().stream().map(this::toDefinition).collect(Collectors.toList()))
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedBy(entity.getUpdatedBy())
        .build();
  }
}
