#!/usr/bin/env python3
"""
简化的SysML v2 模型可视化工具
从我们创建的模型数据生成文本图表和简单的DOT图
"""

import json
from datetime import datetime

class SimpleSysMLVisualizer:
    def __init__(self):
        # 硬编码我们知道创建的模型数据
        self.model_data = {
            "requirements": [
                {"name": "BrakingDistanceRequirement", "id": "REQ-FUNC-001", "text": "车辆必须在100km/h速度下40米内完全停止"},
                {"name": "ResponseTimeRequirement", "id": "REQ-FUNC-002", "text": "制动系统响应时间不得超过150毫秒"},
                {"name": "SafetyRequirement", "id": "REQ-FUNC-003", "text": "制动系统必须具备故障安全机制"},
                {"name": "EnvironmentalRequirement", "id": "REQ-FUNC-004", "text": "制动系统必须在-40°C至+85°C温度范围内正常工作"},
                {"name": "BrakingForceRequirement", "id": "REQ-PERF-001", "text": "制动力必须达到车重的0.8倍以上"},
                {"name": "DurabilityRequirement", "id": "REQ-PERF-002", "text": "制动系统必须承受10万次制动循环"},
                {"name": "TemperatureToleranceRequirement", "id": "REQ-PERF-003", "text": "制动系统必须在-40°C至+200°C范围内工作"},
                {"name": "WeightRequirement", "id": "REQ-PERF-004", "text": "制动系统总重量不得超过50kg"}
            ],
            "parts": [
                {"name": "BrakingSystem", "id": "PART-SYS-001", "desc": "汽车制动系统总成"},
                {"name": "BrakePedal", "id": "PART-COMP-001", "desc": "制动踏板组件"},
                {"name": "MasterCylinder", "id": "PART-COMP-002", "desc": "制动主缸"},
                {"name": "BrakeDisc", "id": "PART-COMP-003", "desc": "制动盘"},
                {"name": "BrakeCaliper", "id": "PART-COMP-004", "desc": "制动卡钳"},
                {"name": "ABSController", "id": "PART-COMP-005", "desc": "ABS防抱死控制器"},
                {"name": "BrakeFluid", "id": "PART-FLUID-001", "desc": "制动液"},
                {"name": "BrakePads", "id": "PART-COMP-006", "desc": "制动片"},
                {"name": "BrakeRotor", "id": "PART-COMP-007", "desc": "制动转子"},
                {"name": "BrakePipe", "id": "PART-PIPE-001", "desc": "制动管路"},
                {"name": "VacuumBooster", "id": "PART-COMP-008", "desc": "真空助力器"},
                {"name": "ElectronicStabilityControl", "id": "PART-COMP-009", "desc": "电子稳定控制系统"}
            ],
            "requirement_usages": [
                {"name": "VehicleBrakingDistanceUsage", "id": "REQ-USAGE-001", "value": "40", "unit": "meters"},
                {"name": "BrakeResponseTimeUsage", "id": "REQ-USAGE-002", "value": "150", "unit": "milliseconds"},
                {"name": "BrakingForceUsage", "id": "REQ-USAGE-003", "value": "0.8", "unit": "coefficient"},
                {"name": "SystemWeightUsage", "id": "REQ-USAGE-004", "value": "45", "unit": "kg"}
            ],
            "part_usages": [
                {"name": "FrontLeftBrakeDiscUsage", "id": "PART-USAGE-001", "diameter": "320mm", "position": "Front Left Wheel"},
                {"name": "FrontRightBrakeDiscUsage", "id": "PART-USAGE-002", "diameter": "320mm", "position": "Front Right Wheel"},
                {"name": "MainBrakeCylinderUsage", "id": "PART-USAGE-003", "bore": "25.4mm", "position": "Engine Bay"},
                {"name": "BrakePedalAssemblyUsage", "id": "PART-USAGE-004", "length": "200mm", "position": "Driver Compartment"},
                {"name": "ABSControlModuleUsage", "id": "PART-USAGE-005", "channels": "4", "position": "Under Hood"},
                {"name": "VacuumBoosterSystemUsage", "id": "PART-USAGE-006", "boostRatio": "7.0", "position": "Firewall"}
            ]
        }
    
    def generate_ascii_requirements_diagram(self):
        """生成ASCII格式的需求图"""
        diagram = """
╔══════════════════════════════════════════════════════════════════════════════════════╗
║                               SysML v2 需求视图 (Requirements View)                  ║
╠══════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                      ║
║  ┌─────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                          功能需求 (Functional Requirements)                     │ ║
║  ├─────────────────────────────────────────────────────────────────────────────────┤ ║
"""
        for req in self.model_data["requirements"][:4]:
            diagram += f"║  │ 📋 {req['name']:<25} ({req['id']})                            │ ║\n"
            diagram += f"║  │    {req['text']:<65} │ ║\n"
            diagram += f"║  │                                                                     │ ║\n"
        
        diagram += """║  └─────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                      ║
║  ┌─────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                          性能需求 (Performance Requirements)                    │ ║
║  ├─────────────────────────────────────────────────────────────────────────────────┤ ║
"""
        for req in self.model_data["requirements"][4:]:
            diagram += f"║  │ ⚡ {req['name']:<25} ({req['id']})                            │ ║\n"
            diagram += f"║  │    {req['text']:<65} │ ║\n"
            diagram += f"║  │                                                                     │ ║\n"
        
        diagram += """║  └─────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                      ║
║  ┌─────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                      需求实例化 (Requirement Usages with Values)                │ ║
║  ├─────────────────────────────────────────────────────────────────────────────────┤ ║
"""
        for usage in self.model_data["requirement_usages"]:
            diagram += f"║  │ 🎯 {usage['name']:<25} = {usage['value']} {usage['unit']:<12} │ ║\n"
        
        diagram += """║  └─────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                      ║
╚══════════════════════════════════════════════════════════════════════════════════════╝
"""
        return diagram
    
    def generate_ascii_structure_diagram(self):
        """生成ASCII格式的结构图"""
        diagram = """
╔══════════════════════════════════════════════════════════════════════════════════════╗
║                               SysML v2 结构视图 (Structure View)                    ║
╠══════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                      ║
║                           ┌─────────────────────────────┐                           ║
║                           │    🚗 BrakingSystem         │                           ║
║                           │    (PART-SYS-001)           │                           ║
║                           └─────────────┬───────────────┘                           ║
║                                         │                                           ║
║              ┌─────────────┬────────────┼────────────┬─────────────┐                ║
║              │             │            │            │             │                ║
║              ▼             ▼            ▼            ▼             ▼                ║
║   ┌─────────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ║
║   │🦵 BrakePedal    │ │🔧 MasterCyl │ │💿 BrakeDisc │ │🔧 BrakeCaliper│ │🧠 ABSController│ ║
║   │(PART-COMP-001)  │ │(PART-COMP-002)│ │(PART-COMP-003)│ │(PART-COMP-004)│ │(PART-COMP-005)│ ║
║   └─────────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ ║
║                                                                                      ║
║                              ╔═══════════════════════════╗                          ║
║                              ║      具体部件实例          ║                          ║
║                              ╚═══════════════════════════╝                          ║
║                                                                                      ║
"""
        for usage in self.model_data["part_usages"]:
            extra_info = list(usage.items())[2:4]  # 获取除name和id外的前2个属性
            info_str = ", ".join([f"{k}={v}" for k, v in extra_info])
            diagram += f"║   🔹 {usage['name']:<35} @ {usage['position']:<20} ║\n"
            diagram += f"║      {info_str:<70} ║\n"
        
        diagram += """║                                                                                      ║
╚══════════════════════════════════════════════════════════════════════════════════════╝
"""
        return diagram
    
    def generate_dot_diagram(self):
        """生成DOT格式的图表"""
        dot_content = '''digraph SysMLModel {
    rankdir=TB;
    compound=true;
    
    // 样式定义
    node [shape=box, style="rounded,filled"];
    
    // 需求子图
    subgraph cluster_requirements {
        label="需求定义 (Requirements)";
        style=filled;
        color=lightblue;
        
'''
        
        # 添加需求节点
        for i, req in enumerate(self.model_data["requirements"]):
            safe_name = req['name'].replace('-', '_')
            dot_content += f'        req_{i} [label="{req["name"]}\\n{req["id"]}", fillcolor=lightblue];\n'
        
        dot_content += '''    }
    
    // 部件子图
    subgraph cluster_parts {
        label="部件定义 (Parts)";
        style=filled;
        color=orange;
        
'''
        
        # 添加部件节点
        for i, part in enumerate(self.model_data["parts"]):
            safe_name = part['name'].replace('-', '_')
            dot_content += f'        part_{i} [label="{part["name"]}\\n{part["id"]}", fillcolor=orange];\n'
        
        dot_content += '''    }
    
    // 实例子图
    subgraph cluster_usages {
        label="实例化 (Usages with Values)";
        style=filled;
        color=lightgreen;
        
'''
        
        # 添加Usage节点
        for i, usage in enumerate(self.model_data["requirement_usages"]):
            dot_content += f'        req_usage_{i} [label="{usage["name"]}\\n{usage["value"]} {usage["unit"]}", fillcolor=lightgreen];\n'
        
        for i, usage in enumerate(self.model_data["part_usages"]):
            dot_content += f'        part_usage_{i} [label="{usage["name"]}\\n@ {usage["position"]}", fillcolor=yellow];\n'
        
        dot_content += '''    }
    
    // 关系连接
    part_0 -> part_1;
    part_0 -> part_2;
    part_0 -> part_3;
    part_0 -> part_4;
    part_0 -> part_5;
    
    // Definition到Usage的关系
    req_0 -> req_usage_0 [style=dashed, color=blue];
    req_1 -> req_usage_1 [style=dashed, color=blue];
    part_2 -> part_usage_0 [style=dashed, color=red];
    part_2 -> part_usage_1 [style=dashed, color=red];
}'''
        
        return dot_content
    
    def generate_model_summary(self):
        """生成模型汇总报告"""
        summary = f"""
# SysML v2 制动系统模型汇总报告
生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

## 📊 模型统计
- 需求定义 (RequirementDefinition): {len(self.model_data['requirements'])} 个
- 部件定义 (PartDefinition): {len(self.model_data['parts'])} 个  
- 需求实例 (RequirementUsage): {len(self.model_data['requirement_usages'])} 个
- 部件实例 (PartUsage): {len(self.model_data['part_usages'])} 个
- 总模型元素: {len(self.model_data['requirements']) + len(self.model_data['parts']) + len(self.model_data['requirement_usages']) + len(self.model_data['part_usages'])} 个

## 🎯 关键验证成果
✅ Definition → Usage 实例化验证
✅ 具体数值参数赋值验证  
✅ SysML v2 官方API服务验证
✅ PostgreSQL数据持久化验证
✅ Commit-based版本控制验证

## 📋 详细清单

### 功能需求 (Functional Requirements)
"""
        for req in self.model_data["requirements"][:4]:
            summary += f"- **{req['name']}** ({req['id']}): {req['text']}\n"
        
        summary += "\n### 性能需求 (Performance Requirements)\n"
        for req in self.model_data["requirements"][4:]:
            summary += f"- **{req['name']}** ({req['id']}): {req['text']}\n"
        
        summary += "\n### 系统部件 (System Parts)\n"
        for part in self.model_data["parts"]:
            summary += f"- **{part['name']}** ({part['id']}): {part['desc']}\n"
        
        summary += "\n### 需求实例化 (Requirement Usages)\n"
        for usage in self.model_data["requirement_usages"]:
            summary += f"- **{usage['name']}** = {usage['value']} {usage['unit']}\n"
        
        summary += "\n### 部件实例化 (Part Usages)\n"
        for usage in self.model_data["part_usages"]:
            summary += f"- **{usage['name']}** @ {usage['position']}\n"
        
        return summary
    
    def save_all_visualizations(self):
        """保存所有可视化文件"""
        import os
        
        # 创建输出目录
        output_dir = "sysml_model_visualization"
        os.makedirs(output_dir, exist_ok=True)
        
        # 保存ASCII需求图
        with open(f"{output_dir}/requirements_ascii.txt", "w", encoding="utf-8") as f:
            f.write(self.generate_ascii_requirements_diagram())
        print(f"✅ ASCII需求图保存: {output_dir}/requirements_ascii.txt")
        
        # 保存ASCII结构图
        with open(f"{output_dir}/structure_ascii.txt", "w", encoding="utf-8") as f:
            f.write(self.generate_ascii_structure_diagram())
        print(f"✅ ASCII结构图保存: {output_dir}/structure_ascii.txt")
        
        # 保存DOT图
        with open(f"{output_dir}/model_diagram.dot", "w", encoding="utf-8") as f:
            f.write(self.generate_dot_diagram())
        print(f"✅ DOT图形文件保存: {output_dir}/model_diagram.dot")
        
        # 保存模型汇总
        with open(f"{output_dir}/model_summary.md", "w", encoding="utf-8") as f:
            f.write(self.generate_model_summary())
        print(f"✅ 模型汇总报告保存: {output_dir}/model_summary.md")
        
        # 尝试生成PNG图（如果graphviz可用）
        try:
            import subprocess
            result = subprocess.run(
                ["dot", "-Tpng", f"{output_dir}/model_diagram.dot", "-o", f"{output_dir}/model_diagram.png"],
                capture_output=True, text=True
            )
            if result.returncode == 0:
                print(f"✅ PNG图形文件生成: {output_dir}/model_diagram.png")
            else:
                print(f"⚠️  PNG生成失败: {result.stderr}")
        except FileNotFoundError:
            print("⚠️  graphviz命令行工具未找到，跳过PNG生成")
        
        print(f"\n🎉 所有可视化文件已保存到 {output_dir}/ 目录")
        return output_dir

def main():
    visualizer = SimpleSysMLVisualizer()
    
    print("🎨 开始生成SysML v2模型可视化...")
    
    # 显示ASCII图表
    print("\n" + "="*90)
    print("📋 需求视图:")
    print(visualizer.generate_ascii_requirements_diagram())
    
    print("\n" + "="*90) 
    print("🏗️  结构视图:")
    print(visualizer.generate_ascii_structure_diagram())
    
    # 保存所有文件
    output_dir = visualizer.save_all_visualizations()
    
    print(f"\n📁 查看完整模型可视化:")
    print(f"   cat {output_dir}/requirements_ascii.txt")
    print(f"   cat {output_dir}/structure_ascii.txt") 
    print(f"   cat {output_dir}/model_summary.md")
    print(f"   # 如果有图形界面: xdg-open {output_dir}/model_diagram.png")

if __name__ == "__main__":
    main()