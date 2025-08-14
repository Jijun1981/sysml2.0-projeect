#!/usr/bin/env python3
"""
SysML v2 模型渲染工具
从SysML-v2-API-Services获取模型数据并生成可视化图表
"""

import requests
import json
import graphviz
from datetime import datetime
import os

class SysMLModelRenderer:
    def __init__(self, base_url="http://localhost:9000", project_id=None):
        self.base_url = base_url
        self.project_id = project_id
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        
    def get_all_elements_from_project(self):
        """获取项目中所有commit的所有元素"""
        all_elements = []
        
        # 获取所有commits
        commits_response = self.session.get(f"{self.base_url}/projects/{self.project_id}/commits")
        if commits_response.status_code != 200:
            print(f"无法获取commits: {commits_response.status_code}")
            return []
            
        commits = commits_response.json()
        print(f"找到 {len(commits)} 个commits")
        
        # 遍历每个commit获取元素
        for commit in commits:
            commit_id = commit.get("@id")
            if commit_id:
                elements_response = self.session.get(
                    f"{self.base_url}/projects/{self.project_id}/commits/{commit_id}/elements"
                )
                if elements_response.status_code == 200:
                    elements = elements_response.json()
                    if elements:
                        print(f"  Commit {commit_id[:8]}...: {len(elements)} 个元素")
                        all_elements.extend(elements)
                    
        return all_elements
    
    def categorize_elements(self, elements):
        """将元素按类型分类"""
        categories = {
            "requirements": [],
            "parts": [], 
            "constraints": [],
            "usages": [],
            "relationships": [],
            "others": []
        }
        
        for element in elements:
            element_type = element.get("@type", "")
            
            if "Requirement" in element_type:
                if "Usage" in element_type:
                    categories["usages"].append(element)
                else:
                    categories["requirements"].append(element)
            elif "Part" in element_type:
                if "Usage" in element_type:
                    categories["usages"].append(element)
                else:
                    categories["parts"].append(element)
            elif "Constraint" in element_type:
                if "Usage" in element_type:
                    categories["usages"].append(element)
                else:
                    categories["constraints"].append(element)
            elif "Satisfy" in element_type or "Verify" in element_type:
                categories["relationships"].append(element)
            else:
                categories["others"].append(element)
                
        return categories
    
    def render_requirements_diagram(self, requirements, usages):
        """渲染需求图"""
        dot = graphviz.Digraph(comment='SysML Requirements Diagram')
        dot.attr(rankdir='TB', size='12,8')
        dot.attr('node', shape='box', style='rounded,filled')
        
        # 需求定义 (蓝色)
        with dot.subgraph(name='cluster_requirements') as req_cluster:
            req_cluster.attr(label='Requirement Definitions', style='filled', color='lightblue')
            
            for req in requirements:
                req_name = req.get("declaredName", "Unknown")
                req_id = req.get("identifier", req.get("@id", "")[:8])
                req_text = req.get("text", "")[:50] + "..." if len(req.get("text", "")) > 50 else req.get("text", "")
                
                req_cluster.node(
                    req_id, 
                    f"{req_name}\\n{req_id}\\n{req_text}",
                    fillcolor='lightblue'
                )
        
        # 需求使用 (绿色)
        with dot.subgraph(name='cluster_usages') as usage_cluster:
            usage_cluster.attr(label='Requirement Usages', style='filled', color='lightgreen')
            
            for usage in usages:
                if "Requirement" in usage.get("@type", ""):
                    usage_name = usage.get("declaredName", "Unknown")
                    usage_id = usage.get("identifier", usage.get("@id", "")[:8])
                    
                    # 提取数值信息
                    value_info = ""
                    if "value" in usage:
                        value_data = usage["value"]
                        if isinstance(value_data, dict):
                            literal_val = value_data.get("literalValue", "")
                            unit = value_data.get("unit", "")
                            value_info = f"\\nValue: {literal_val} {unit}"
                    
                    usage_cluster.node(
                        usage_id,
                        f"{usage_name}\\n{usage_id}{value_info}",
                        fillcolor='lightgreen'
                    )
        
        return dot
    
    def render_structure_diagram(self, parts, usages):
        """渲染结构图"""
        dot = graphviz.Digraph(comment='SysML Structure Diagram')
        dot.attr(rankdir='LR', size='14,10')
        dot.attr('node', shape='component', style='filled')
        
        # 部件定义 (橙色)
        with dot.subgraph(name='cluster_parts') as part_cluster:
            part_cluster.attr(label='Part Definitions', style='filled', color='orange')
            
            for part in parts:
                part_name = part.get("declaredName", "Unknown")
                part_id = part.get("identifier", part.get("@id", "")[:8])
                
                part_cluster.node(
                    part_id,
                    f"{part_name}\\n{part_id}",
                    fillcolor='orange'
                )
        
        # 部件使用 (黄色)
        with dot.subgraph(name='cluster_part_usages') as usage_cluster:
            usage_cluster.attr(label='Part Usages', style='filled', color='yellow')
            
            for usage in usages:
                if "Part" in usage.get("@type", ""):
                    usage_name = usage.get("declaredName", "Unknown")
                    usage_id = usage.get("identifier", usage.get("@id", "")[:8])
                    
                    # 提取属性信息
                    attr_info = ""
                    if "attributes" in usage:
                        attrs = usage["attributes"]
                        if isinstance(attrs, dict):
                            key_attrs = []
                            for key, value in list(attrs.items())[:2]:  # 只显示前2个属性
                                key_attrs.append(f"{key}: {value}")
                            if key_attrs:
                                attr_info = "\\n" + "\\n".join(key_attrs)
                    
                    usage_cluster.node(
                        usage_id,
                        f"{usage_name}\\n{usage_id}{attr_info}",
                        fillcolor='yellow'
                    )
        
        return dot
    
    def render_constraint_diagram(self, constraints, usages):
        """渲染约束图"""
        dot = graphviz.Digraph(comment='SysML Constraint Diagram')
        dot.attr(rankdir='TB', size='10,8')
        dot.attr('node', shape='ellipse', style='filled')
        
        # 约束定义 (紫色)
        for constraint in constraints:
            const_name = constraint.get("declaredName", "Unknown")
            const_id = constraint.get("identifier", constraint.get("@id", "")[:8])
            
            dot.node(
                const_id,
                f"{const_name}\\n{const_id}",
                fillcolor='lavender'
            )
        
        # 约束使用 (粉色)
        for usage in usages:
            if "Constraint" in usage.get("@type", ""):
                usage_name = usage.get("declaredName", "Unknown")
                usage_id = usage.get("identifier", usage.get("@id", "")[:8])
                
                # 提取约束参数
                param_info = ""
                if "parameters" in usage:
                    params = usage["parameters"]
                    if isinstance(params, dict):
                        key_params = []
                        for key, value in list(params.items())[:2]:
                            key_params.append(f"{key}: {value}")
                        if key_params:
                            param_info = "\\n" + "\\n".join(key_params)
                
                dot.node(
                    usage_id,
                    f"{usage_name}\\n{usage_id}{param_info}",
                    fillcolor='pink'
                )
        
        return dot
    
    def generate_model_report(self, elements):
        """生成模型报告"""
        categories = self.categorize_elements(elements)
        
        report = f"""
# SysML v2 模型渲染报告
生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
项目ID: {self.project_id}

## 模型统计
- 总元素数量: {len(elements)}
- 需求定义: {len(categories['requirements'])}
- 部件定义: {len(categories['parts'])}
- 约束定义: {len(categories['constraints'])}
- 使用实例: {len(categories['usages'])}
- 关系元素: {len(categories['relationships'])}
- 其他元素: {len(categories['others'])}

## 详细清单

### 需求定义 (RequirementDefinition)
"""
        for req in categories['requirements']:
            req_name = req.get("declaredName", "Unknown")
            req_id = req.get("identifier", "")
            req_text = req.get("text", "")
            report += f"- {req_name} ({req_id}): {req_text}\n"
        
        report += "\n### 部件定义 (PartDefinition)\n"
        for part in categories['parts']:
            part_name = part.get("declaredName", "Unknown")
            part_id = part.get("identifier", "")
            part_doc = part.get("documentation", "")
            report += f"- {part_name} ({part_id}): {part_doc}\n"
        
        report += "\n### 使用实例 (Usage Elements)\n"
        for usage in categories['usages']:
            usage_name = usage.get("declaredName", "Unknown")
            usage_type = usage.get("@type", "")
            usage_id = usage.get("identifier", "")
            report += f"- {usage_name} ({usage_type}, {usage_id})\n"
        
        return report
    
    def render_all_diagrams(self):
        """渲染所有图表"""
        print("🎨 开始渲染SysML v2模型...")
        
        # 获取所有元素
        elements = self.get_all_elements_from_project()
        if not elements:
            print("❌ 未找到任何模型元素")
            return
            
        print(f"✅ 找到 {len(elements)} 个模型元素")
        
        # 分类元素
        categories = self.categorize_elements(elements)
        
        # 创建输出目录
        output_dir = "sysml_model_diagrams"
        os.makedirs(output_dir, exist_ok=True)
        
        # 渲染需求图
        if categories['requirements'] or any("Requirement" in u.get("@type", "") for u in categories['usages']):
            req_usages = [u for u in categories['usages'] if "Requirement" in u.get("@type", "")]
            req_diagram = self.render_requirements_diagram(categories['requirements'], req_usages)
            req_diagram.render(f'{output_dir}/requirements_diagram', format='png', cleanup=True)
            print(f"✅ 需求图已保存: {output_dir}/requirements_diagram.png")
        
        # 渲染结构图
        if categories['parts'] or any("Part" in u.get("@type", "") for u in categories['usages']):
            part_usages = [u for u in categories['usages'] if "Part" in u.get("@type", "")]
            struct_diagram = self.render_structure_diagram(categories['parts'], part_usages)
            struct_diagram.render(f'{output_dir}/structure_diagram', format='png', cleanup=True)
            print(f"✅ 结构图已保存: {output_dir}/structure_diagram.png")
        
        # 渲染约束图
        if categories['constraints'] or any("Constraint" in u.get("@type", "") for u in categories['usages']):
            const_usages = [u for u in categories['usages'] if "Constraint" in u.get("@type", "")]
            const_diagram = self.render_constraint_diagram(categories['constraints'], const_usages)
            const_diagram.render(f'{output_dir}/constraint_diagram', format='png', cleanup=True)
            print(f"✅ 约束图已保存: {output_dir}/constraint_diagram.png")
        
        # 生成模型报告
        report = self.generate_model_report(elements)
        with open(f'{output_dir}/model_report.md', 'w', encoding='utf-8') as f:
            f.write(report)
        print(f"✅ 模型报告已保存: {output_dir}/model_report.md")
        
        print(f"\n🎉 模型渲染完成！所有文件保存在 {output_dir}/ 目录中")

def main():
    # 使用最新的项目ID
    project_id = "8b46c38d-8cdb-40e5-be1f-ec7362cd02f2"
    
    renderer = SysMLModelRenderer(project_id=project_id)
    renderer.render_all_diagrams()

if __name__ == "__main__":
    main()