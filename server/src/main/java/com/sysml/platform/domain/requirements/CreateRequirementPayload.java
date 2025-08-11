package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;

/**
 * 创建需求返回载荷
 */
@Data
@Builder
public class CreateRequirementPayload {
    private boolean ok;
    private Error error;
    private RequirementDefinition requirement;
}