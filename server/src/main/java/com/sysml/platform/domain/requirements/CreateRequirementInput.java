package com.sysml.platform.domain.requirements;

import lombok.Data;

@Data
public class CreateRequirementInput {
  private String reqId;
  private String name;
  private String text;
  private String parentId;
}

