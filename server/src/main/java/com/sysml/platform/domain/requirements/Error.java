package com.sysml.platform.domain.requirements;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 错误信息
 */
@Data
@Builder
public class Error {
    private String code;
    private String messageKey;
    private List<String> path;
}