package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 查询需求返回载荷
 */
@Data
@Builder
public class QueryRequirementsPayload {
    private boolean ok;
    private Error error;
    private List<RequirementDefinition> content;
    private int totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;
}