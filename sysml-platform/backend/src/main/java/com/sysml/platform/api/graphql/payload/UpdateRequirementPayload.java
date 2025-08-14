package com.sysml.platform.api.graphql.payload;

import com.sysml.platform.domain.requirements.RequirementDefinition;
import lombok.Builder;
import lombok.Data;

/** 更新需求的响应Payload */
@Data
@Builder
public class UpdateRequirementPayload {
  private boolean ok;
  private Error error;
  private RequirementDefinition requirement;
}
