package com.sysml.platform.domain.requirements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal placeholder model to unblock compilation and tests. Aligns with API contract fields for
 * RequirementDefinition.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementDefinition {
  private String id;
  private String reqId;
  private String name;
  private String text;
  private RequirementDefinition parent;
  @Builder.Default private List<RequirementDefinition> children = new ArrayList<>();
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;
  private String updatedBy;
}
