package com.sysml.platform.domain.requirements;

import lombok.Data;

@Data
public class UpdateRequirementInput {
  private String name;
  private String text;
  private String parentId; // 可以是UUID或"null"表示移除父节点
}
