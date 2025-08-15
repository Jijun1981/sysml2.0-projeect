package com.sysml.platform.domain.requirements;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementsPage {
  private List<RequirementDefinition> items;
  private PageInfo pageInfo;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PageInfo {
    private int total;
    private int page;
    private int size;
    private boolean hasNext;
    private boolean hasPrevious;
  }
}
