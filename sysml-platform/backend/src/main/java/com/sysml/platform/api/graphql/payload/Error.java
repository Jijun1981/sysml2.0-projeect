package com.sysml.platform.api.graphql.payload;

import lombok.Builder;
import lombok.Data;

/** GraphQL错误模型 */
@Data
@Builder
public class Error {
  private String code;
  private String messageKey;
  private String details;
}
