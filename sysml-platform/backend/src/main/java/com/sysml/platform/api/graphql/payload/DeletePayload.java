package com.sysml.platform.api.graphql.payload;

import lombok.Builder;
import lombok.Data;

/** 删除操作的响应Payload */
@Data
@Builder
public class DeletePayload {
  private boolean ok;
  private Error error;
  private String deletedId;
}
