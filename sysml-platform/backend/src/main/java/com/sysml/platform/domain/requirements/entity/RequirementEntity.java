package com.sysml.platform.domain.requirements.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 需求实体 - 使用JSONB存储扩展属性 */
@Entity
@Table(name = "requirements")
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"parent", "children"})
public class RequirementEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(unique = true, nullable = false, length = 100)
  private String reqId; // 业务唯一标识

  @Column(nullable = false, length = 255)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String text;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private RequirementEntity parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RequirementEntity> children = new ArrayList<>();

  // 层次关系类型
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private HierarchyType hierarchyType;

  // JSONB扩展属性
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes = new HashMap<>();

  // 审计字段
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(length = 100)
  private String createdBy;

  @Column(length = 100)
  private String updatedBy;

  @Version private Long version; // 乐观锁

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum HierarchyType {
    DERIVE,
    REFINE,
    DECOMPOSE
  }
}
