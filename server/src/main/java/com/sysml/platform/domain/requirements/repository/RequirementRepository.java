package com.sysml.platform.domain.requirements.repository;

import com.sysml.platform.domain.requirements.entity.RequirementEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 需求Repository - JPA数据访问层 */
@Repository
public interface RequirementRepository extends JpaRepository<RequirementEntity, UUID> {

  /** 根据业务ID查找需求 */
  Optional<RequirementEntity> findByReqId(String reqId);

  /** 检查业务ID是否存在 */
  boolean existsByReqId(String reqId);

  /** 根据父需求查找子需求 */
  List<RequirementEntity> findByParentId(UUID parentId);

  /** 查找根需求（无父需求） */
  @Query("SELECT r FROM RequirementEntity r WHERE r.parent IS NULL")
  Page<RequirementEntity> findRootRequirements(Pageable pageable);

  /** 检查是否会形成循环依赖 使用递归CTE检查从child到parent的路径是否已存在 */
  @Query(
      value =
          """
        WITH RECURSIVE req_path AS (
            SELECT id, parent_id FROM requirements WHERE id = :childId
            UNION ALL
            SELECT r.id, r.parent_id
            FROM requirements r
            INNER JOIN req_path rp ON r.id = rp.parent_id
        )
        SELECT COUNT(*) > 0 FROM req_path WHERE id = :parentId
        """,
      nativeQuery = true)
  boolean wouldCreateCycle(@Param("childId") UUID childId, @Param("parentId") UUID parentId);

  /** 获取需求的所有祖先 */
  @Query(
      value =
          """
        WITH RECURSIVE ancestors AS (
            SELECT * FROM requirements WHERE id = :reqId
            UNION ALL
            SELECT r.*
            FROM requirements r
            INNER JOIN ancestors a ON r.id = a.parent_id
        )
        SELECT * FROM ancestors WHERE id != :reqId
        """,
      nativeQuery = true)
  List<RequirementEntity> findAncestors(@Param("reqId") UUID reqId);

  /** 获取需求的所有后代 */
  @Query(
      value =
          """
        WITH RECURSIVE descendants AS (
            SELECT * FROM requirements WHERE id = :reqId
            UNION ALL
            SELECT r.*
            FROM requirements r
            INNER JOIN descendants d ON r.parent_id = d.id
        )
        SELECT * FROM descendants WHERE id != :reqId
        """,
      nativeQuery = true)
  List<RequirementEntity> findDescendants(@Param("reqId") UUID reqId);

  /** 根据名称模糊搜索 */
  Page<RequirementEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

  /** 根据文本内容搜索 */
  @Query(
      "SELECT r FROM RequirementEntity r WHERE LOWER(r.text) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  Page<RequirementEntity> searchByText(@Param("keyword") String keyword, Pageable pageable);

  /** 使用JSONB属性查询 */
  @Query(value = "SELECT * FROM requirements WHERE attributes @> :attributes", nativeQuery = true)
  List<RequirementEntity> findByAttributes(@Param("attributes") String attributesJson);
}
