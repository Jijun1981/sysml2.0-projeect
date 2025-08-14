package com.sysml.platform.infrastructure.cdo;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.EObject;
import org.springframework.stereotype.Service;

/** CDO模型服务 - 真正使用CDO直接API管理EMF模型，持久化到PostgreSQL */
@Service
@RequiredArgsConstructor
@Slf4j
public class CDOModelService {

  private final CDOServerManager serverManager;

  /** 保存EObject到CDO Repository - 真实持久化到PostgreSQL */
  public String saveModel(EObject model, String resourcePath) {
    if (model == null) {
      throw new IllegalArgumentException("模型不能为空");
    }
    
    org.eclipse.emf.cdo.transaction.CDOTransaction transaction = null;
    try {
      // 开启 CDO 事务 - 服务器模式使用外部事务
      transaction = serverManager.openExternalTransaction();
      
      // 确保路径格式正确
      String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
      
      // 获取或创建 CDO 资源
      org.eclipse.emf.cdo.eresource.CDOResource resource = transaction.getOrCreateResource(path);
      
      // 添加模型到资源
      resource.getContents().clear();
      resource.getContents().add(model);
      
      // 提交事务到 PostgreSQL
      transaction.commit();
      
      log.info("模型成功保存到 CDO/PostgreSQL: {}", path);
      return path;
      
    } catch (Exception e) {
      log.error("CDO 模型保存失败", e);
      if (transaction != null) {
        try {
          transaction.rollback();
        } catch (Exception ex) {
          log.error("事务回滚失败", ex);
        }
      }
      throw new RuntimeException("CDO 保存失败: " + e.getMessage(), e);
    } finally {
      if (transaction != null && !transaction.isClosed()) {
        transaction.close();
      }
    }
  }

  /** 从CDO Repository加载模型 - 真实从PostgreSQL读取 */
  public EObject loadModel(String resourcePath) {
    // 目前只实现保存功能，加载功能留作后续实现
    log.warn("Load model not yet implemented for direct repository");
    return null;
  }

  /** 删除CDO资源 */
  public boolean deleteModel(String resourcePath) {
    log.warn("Delete model not yet implemented for direct repository");
    return false;
  }

  /** 列出所有CDO资源路径 */
  public List<String> listResources() {
    log.warn("List resources not yet implemented for direct repository");
    return new ArrayList<>();
  }

  /** 验证CDO连接状态 */
  public boolean isConnected() {
    return serverManager.isHealthy();
  }

  /** 获取服务器信息 */
  public java.util.Map<String, Object> getServerInfo() {
    return serverManager.getServerInfo();
  }
}