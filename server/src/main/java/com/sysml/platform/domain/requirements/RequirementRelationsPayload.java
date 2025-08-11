package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

/**
 * 需求关系返回载荷
 */
@Data
@Builder
public class RequirementRelationsPayload {
    private boolean ok;
    private Error error;
    private String requirementId;
    private Set<String> derives;
    private Set<String> refines;
    private Set<String> derivedFrom;
    private Set<String> refinedBy;
}