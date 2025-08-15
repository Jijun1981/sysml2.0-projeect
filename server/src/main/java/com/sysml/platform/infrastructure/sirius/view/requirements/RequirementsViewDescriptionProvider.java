package com.sysml.platform.infrastructure.sirius.view.requirements;

import com.sysml.platform.infrastructure.sirius.view.requirements.services.RequirementsColorProvider;
import java.util.List;
import java.util.UUID;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.components.emf.ResourceMetadataAdapter;
import org.eclipse.sirius.components.emf.services.IDAdapter;
import org.eclipse.sirius.components.emf.services.JSONResourceFactory;
import org.eclipse.sirius.components.view.RepresentationDescription;
import org.eclipse.sirius.components.view.View;
import org.eclipse.sirius.components.view.builder.generated.view.ViewBuilder;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;
import org.eclipse.sirius.emfjson.resource.JsonResource;
import org.springframework.stereotype.Service;

/** 需求视图描述提供者 - 基于Sirius Web基础API实现 满足EP-REQ的RQ-REQ-UI-006和RQ-REQ-MODELER-007需求 */
@Service
public class RequirementsViewDescriptionProvider {

  private static final String VIEW_DIAGRAM_ID = "RequirementsViewDiagram";

  public String getViewDiagramId() {
    return VIEW_DIAGRAM_ID;
  }

  public IRepresentationDescriptionProvider getRepresentationDescriptionProvider() {
    return new RequirementsViewDiagramDescriptionProvider();
  }

  /** 获取需求视图的表示描述（仿照SysON的模式） */
  public List<View> getRepresentationDescriptions() {
    // Create org.eclipse.sirius.components.view.View
    ViewBuilder viewBuilder = new ViewBuilder();
    View view = viewBuilder.build();
    IColorProvider colorProvider = new RequirementsColorProvider();

    // Create org.eclipse.sirius.components.view.RepresentationDescription
    IRepresentationDescriptionProvider viewDiagramDescriptionProvider =
        this.getRepresentationDescriptionProvider();
    RepresentationDescription viewRepresentationDescription =
        viewDiagramDescriptionProvider.create(colorProvider);
    view.getDescriptions().add(viewRepresentationDescription);

    // Add an ID to all view elements
    view.eAllContents()
        .forEachRemaining(
            eObject -> {
              eObject
                  .eAdapters()
                  .add(
                      new IDAdapter(
                          UUID.nameUUIDFromBytes(EcoreUtil.getURI(eObject).toString().getBytes())));
            });

    // All programmatic Views need to be stored in a Resource
    String resourcePath = UUID.nameUUIDFromBytes(this.getViewDiagramId().getBytes()).toString();
    JsonResource resource = new JSONResourceFactory().createResourceFromPath(resourcePath);
    resource.eAdapters().add(new ResourceMetadataAdapter(this.getViewDiagramId()));
    resource.getContents().add(view);

    return List.of(view);
  }
}
