package com.sysml.platform.infrastructure.sirius.view.requirements;

import org.eclipse.sirius.components.view.RepresentationDescription;
import org.eclipse.sirius.components.view.builder.generated.diagram.DiagramBuilders;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;

/** Requirements View图表描述提供者 - 基于SysON的DiagramBuilders API 满足EP-REQ需求的RQ-REQ-UI-006：需求视图可用 */
public class RequirementsViewDiagramDescriptionProvider
    implements IRepresentationDescriptionProvider {

  public static final String DESCRIPTION_NAME = "Requirements View";

  private final DiagramBuilders diagramBuilderHelper = new DiagramBuilders();

  @Override
  public RepresentationDescription create(IColorProvider colorProvider) {

    // 使用DiagramBuilders创建图表描述，严格按照SysON的API
    var diagramDescription =
        this.diagramBuilderHelper
            .newDiagramDescription()
            .autoLayout(false)
            .domainType("sysml::Namespace")
            .preconditionExpression("aql:true")
            .name(DESCRIPTION_NAME)
            .titleExpression(DESCRIPTION_NAME)
            .build();

    // 创建需求定义节点，按照SysON模式
    var requirementDefInsideLabel =
        this.diagramBuilderHelper
            .newInsideLabelDescription()
            .labelExpression("aql:self.name")
            .build();

    var requirementDefNodeStyle =
        this.diagramBuilderHelper
            .newRectangularNodeStyleDescription()
            .background(colorProvider.getColor("color_requirement_def"))
            .borderColor(colorProvider.getColor("border_requirement"))
            .borderRadius(0)
            .build();

    var requirementDefNode =
        this.diagramBuilderHelper
            .newNodeDescription()
            .name("RequirementDefinition")
            .domainType("sysml::RequirementDefinition")
            .semanticCandidatesExpression(
                "aql:self.eAllContents()->filter(sysml::RequirementDefinition)")
            .insideLabel(requirementDefInsideLabel)
            .style(requirementDefNodeStyle)
            .build();

    diagramDescription.getNodeDescriptions().add(requirementDefNode);

    // 创建需求使用节点
    var requirementUsageInsideLabel =
        this.diagramBuilderHelper
            .newInsideLabelDescription()
            .labelExpression("aql:self.name")
            .build();

    var requirementUsageNodeStyle =
        this.diagramBuilderHelper
            .newRectangularNodeStyleDescription()
            .background(colorProvider.getColor("color_requirement_usage"))
            .borderColor(colorProvider.getColor("border_requirement"))
            .borderRadius(0)
            .build();

    var requirementUsageNode =
        this.diagramBuilderHelper
            .newNodeDescription()
            .name("RequirementUsage")
            .domainType("sysml::RequirementUsage")
            .semanticCandidatesExpression(
                "aql:self.eAllContents()->filter(sysml::RequirementUsage)")
            .insideLabel(requirementUsageInsideLabel)
            .style(requirementUsageNodeStyle)
            .build();

    diagramDescription.getNodeDescriptions().add(requirementUsageNode);

    // 创建基本的derive关系边，按照SysON模式
    var deriveEdgeStyle =
        this.diagramBuilderHelper
            .newEdgeStyle()
            .color(colorProvider.getColor("color_derive"))
            .build();

    var deriveEdge =
        this.diagramBuilderHelper
            .newEdgeDescription()
            .name("Derive")
            .domainType("sysml::Specialization")
            .semanticCandidatesExpression("aql:self.eAllContents()->filter(sysml::Specialization)")
            .sourceExpression("aql:self.specific")
            .targetExpression("aql:self.general")
            .centerLabelExpression("derive")
            .style(deriveEdgeStyle)
            .build();

    // 设置源和目标节点描述，按照SysON的link模式
    deriveEdge.getSourceDescriptions().add(requirementDefNode);
    deriveEdge.getSourceDescriptions().add(requirementUsageNode);
    deriveEdge.getTargetDescriptions().add(requirementDefNode);
    deriveEdge.getTargetDescriptions().add(requirementUsageNode);

    diagramDescription.getEdgeDescriptions().add(deriveEdge);

    // 创建简单的调色板，不必要过于复杂
    var palette = this.diagramBuilderHelper.newDiagramPalette().build();

    var nodeToolSection =
        this.diagramBuilderHelper.newDiagramToolSection().name("Requirements").build();

    palette.getToolSections().add(nodeToolSection);
    diagramDescription.setPalette(palette);

    return diagramDescription;
  }
}
