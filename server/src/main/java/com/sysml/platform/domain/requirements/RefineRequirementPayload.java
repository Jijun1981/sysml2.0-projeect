package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;

/**
 * 细化需求返回载荷
 */
@Data
@Builder
public class RefineRequirementPayload {
    private boolean ok;
    private Error error;
    private RequirementDefinition source;
    private RequirementDefinition target;
}