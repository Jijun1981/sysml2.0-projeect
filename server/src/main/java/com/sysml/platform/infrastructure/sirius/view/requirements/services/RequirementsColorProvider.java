package com.sysml.platform.infrastructure.sirius.view.requirements.services;

import org.eclipse.sirius.components.view.FixedColor;
import org.eclipse.sirius.components.view.View;
import org.eclipse.sirius.components.view.ViewFactory;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;

/** 需求视图颜色提供者 - 提供符合SysML标准的颜色方案 */
public class RequirementsColorProvider implements IColorProvider {

  private final ViewFactory viewFactory = ViewFactory.eINSTANCE;
  private final View view;

  public RequirementsColorProvider(View view) {
    this.view = view;
  }

  public RequirementsColorProvider() {
    this.view = ViewFactory.eINSTANCE.createView();
  }

  @Override
  public FixedColor getColor(String colorKey) {
    return switch (colorKey) {
        // 需求相关颜色
      case "color_requirement_def" -> this.createColor("RequirementDef", "#E8F5E9");
      case "color_requirement_usage" -> this.createColor("RequirementUsage", "#F3E5F5");
      case "border_requirement" -> this.createColor("RequirementBorder", "#4CAF50");

        // 派生关系颜色
      case "color_derive" -> this.createColor("Derive", "#2196F3");
      case "color_refine" -> this.createColor("Refine", "#9C27B0");
      case "color_satisfy" -> this.createColor("Satisfy", "#FF9800");

        // 包颜色
      case "color_package" -> this.createColor("Package", "#FFF3E0");
      case "border_package" -> this.createColor("PackageBorder", "#795548");

        // 通用颜色
      case "color_white" -> this.createColor("White", "#FFFFFF");
      case "color_black" -> this.createColor("Black", "#000000");
      case "color_gray" -> this.createColor("Gray", "#9E9E9E");
      case "color_light_gray" -> this.createColor("LightGray", "#F5F5F5");

        // 选中和高亮颜色
      case "color_selected" -> this.createColor("Selected", "#2196F3");
      case "color_hover" -> this.createColor("Hover", "#E3F2FD");
      case "color_error" -> this.createColor("Error", "#F44336");
      case "color_warning" -> this.createColor("Warning", "#FF9800");
      case "color_success" -> this.createColor("Success", "#4CAF50");

        // 默认颜色
      default -> this.createColor("Default", "#E0E0E0");
    };
  }

  /** 创建固定颜色并添加到视图中 */
  private FixedColor createColor(String name, String value) {
    FixedColor color = this.viewFactory.createFixedColor();
    color.setName(name);
    color.setValue(value);

    // 添加到视图的颜色列表中
    this.view.getColorPalettes().stream()
        .findFirst()
        .orElseGet(
            () -> {
              var palette = this.viewFactory.createColorPalette();
              palette.setName("Requirements Colors");
              this.view.getColorPalettes().add(palette);
              return palette;
            })
        .getColors()
        .add(color);

    return color;
  }
}
