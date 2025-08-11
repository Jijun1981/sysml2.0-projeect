package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;

/**
 * 创建需求输入
 */
@Data
@Builder
public class CreateRequirementInput {
    private String reqId;
    private String name;
    private String text;
    private RequirementKind kind;
    private RequirementPriority priority;
}