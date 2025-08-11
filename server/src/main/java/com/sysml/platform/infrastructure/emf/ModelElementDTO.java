package com.sysml.platform.infrastructure.emf;

import lombok.Data;

/**
 * 模型元素DTO
 * 用于EMF对象和业务对象之间的映射
 */
@Data
public class ModelElementDTO {
    private String id;
    private String name;
    private String type;
    private String description;
}