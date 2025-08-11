package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;

/**
 * 派生需求返回载荷
 */
@Data
@Builder
public class DeriveRequirementPayload {
    private boolean ok;
    private Error error;
    private RequirementDefinition source;
    private RequirementDefinition target;
}