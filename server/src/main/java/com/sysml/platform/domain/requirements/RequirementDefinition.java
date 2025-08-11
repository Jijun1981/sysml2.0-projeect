package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

/**
 * 需求定义实体
 */
@Data
@Builder
public class RequirementDefinition {
    private String id;
    private String reqId;
    private String name;
    private String text;
    private RequirementKind kind;
    private RequirementPriority priority;
    private RequirementStatus status;
    private Set<String> derivedFrom;  // 层次关系
    private Set<String> derives;
    private Set<String> refines;  // 细化关系
    private Set<String> refinedBy;
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;
}