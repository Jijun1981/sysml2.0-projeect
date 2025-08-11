package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;

/**
 * 查询需求输入
 */
@Data
@Builder
public class QueryRequirementsInput {
    private String keyword;
    private Integer page;
    private Integer size;
    private String sortBy;
}