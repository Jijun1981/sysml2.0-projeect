#!/usr/bin/env python3
"""
多种方式展示制动系统模型数据
"""
import requests
import json
from datetime import datetime
import uuid

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}

def get_all_projects():
    """获取所有项目"""
    response = requests.get(f"{BASE_URL}/projects", headers=headers)
    if response.status_code == 200:
        return response.json()
    return []

def get_project_commits(project_id):
    """获取项目的所有commits"""
    response = requests.get(f"{BASE_URL}/projects/{project_id}/commits", headers=headers)
    if response.status_code == 200:
        return response.json()
    return []

def get_commit_elements(project_id, commit_id):
    """获取commit中的所有元素"""
    response = requests.get(
        f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/elements",
        headers=headers
    )
    if response.status_code == 200:
        return response.json()
    return []

def display_json_format(data, title="JSON Format"):
    """JSON格式展示"""
    print(f"\n{'='*60}")
    print(f"=== {title} ===")
    print('='*60)
    print(json.dumps(data, indent=2, ensure_ascii=False))

def display_table_format(elements):
    """表格格式展示"""
    print("\n" + "="*100)
    print("=== TABLE FORMAT - ELEMENT SUMMARY ===")
    print("="*100)
    
    # 按类型分组
    by_type = {}
    for elem in elements:
        elem_type = elem.get("@type", "Unknown")
        if elem_type not in by_type:
            by_type[elem_type] = []
        by_type[elem_type].append(elem)
    
    # 打印表头
    print(f"{'Type':<30} {'Count':<10} {'Names':<60}")
    print("-"*100)
    
    # 打印每种类型
    for elem_type in sorted(by_type.keys()):
        items = by_type[elem_type]
        names = [item.get("name", "unnamed") for item in items[:3]]
        names_str = ", ".join(names)
        if len(items) > 3:
            names_str += f", ... (+{len(items)-3} more)"
        print(f"{elem_type:<30} {len(items):<10} {names_str:<60}")
    
    print("-"*100)
    print(f"{'TOTAL':<30} {len(elements):<10}")

def display_tree_format(project_id):
    """树形结构展示"""
    print("\n" + "="*80)
    print("=== TREE FORMAT - PROJECT STRUCTURE ===")
    print("="*80)
    
    # 获取项目信息
    response = requests.get(f"{BASE_URL}/projects/{project_id}", headers=headers)
    if response.status_code != 200:
        print("Failed to get project info")
        return
    
    project = response.json()
    print(f"📁 Project: {project.get('name', 'Unknown')}")
    print(f"   ID: {project.get('@id')}")
    print(f"   Created: {project.get('created', 'Unknown')}")
    
    # 获取commits
    commits = get_project_commits(project_id)
    print(f"\n   📋 Commits: {len(commits)}")
    
    # 显示最近的5个commits
    for i, commit in enumerate(commits[:5]):
        commit_id = commit.get("@id")
        created = commit.get("created", "Unknown")
        
        # 获取该commit的元素
        elements = get_commit_elements(project_id, commit_id)
        
        print(f"   {'└──' if i == min(4, len(commits)-1) else '├──'} Commit {i+1}")
        print(f"   {'   ' if i == min(4, len(commits)-1) else '│  '} ID: {commit_id}")
        print(f"   {'   ' if i == min(4, len(commits)-1) else '│  '} Time: {created}")
        print(f"   {'   ' if i == min(4, len(commits)-1) else '│  '} Elements: {len(elements)}")
        
        # 显示元素类型
        if elements:
            types = {}
            for elem in elements:
                t = elem.get("@type", "Unknown")
                types[t] = types.get(t, 0) + 1
            
            for j, (elem_type, count) in enumerate(types.items()):
                is_last = j == len(types) - 1
                print(f"   {'   ' if i == min(4, len(commits)-1) else '│  '} {'└──' if is_last else '├──'} {elem_type}: {count}")
    
    if len(commits) > 5:
        print(f"   ... and {len(commits) - 5} more commits")

def display_statistics(project_id):
    """统计信息展示"""
    print("\n" + "="*80)
    print("=== STATISTICS - MODEL ANALYSIS ===")
    print("="*80)
    
    commits = get_project_commits(project_id)
    
    total_elements = 0
    all_types = {}
    all_names = []
    
    for commit in commits:
        elements = get_commit_elements(project_id, commit["@id"])
        total_elements += len(elements)
        
        for elem in elements:
            elem_type = elem.get("@type", "Unknown")
            all_types[elem_type] = all_types.get(elem_type, 0) + 1
            
            name = elem.get("name")
            if name:
                all_names.append(name)
    
    print(f"\n📊 Overall Statistics:")
    print(f"   • Total Commits: {len(commits)}")
    print(f"   • Total Elements: {total_elements}")
    print(f"   • Unique Element Types: {len(all_types)}")
    print(f"   • Named Elements: {len(all_names)}")
    
    print(f"\n📈 Top Element Types:")
    sorted_types = sorted(all_types.items(), key=lambda x: x[1], reverse=True)
    for i, (elem_type, count) in enumerate(sorted_types[:10]):
        bar = "█" * min(50, count)
        print(f"   {i+1:2}. {elem_type:<30} {count:3} {bar}")
    
    print(f"\n🏷️ Element Categories:")
    categories = {
        "Requirements": ["RequirementDefinition", "RequirementUsage", "SatisfyRequirementUsage"],
        "Parts": ["PartDefinition", "PartUsage"],
        "Ports": ["PortDefinition", "PortUsage"],
        "Connections": ["ConnectionDefinition", "ConnectionUsage", "Connector", "BindingConnector"],
        "Interfaces": ["InterfaceDefinition", "InterfaceUsage"],
        "Constraints": ["ConstraintDefinition", "ConstraintUsage"],
        "Actions": ["ActionDefinition", "ActionUsage"],
        "States": ["StateDefinition", "StateUsage"],
        "Cases": ["UseCaseDefinition", "UseCaseUsage", "VerificationCaseDefinition", "AnalysisCaseDefinition"],
        "Views": ["ViewDefinition", "ViewUsage"],
        "Allocations": ["AllocationDefinition", "AllocationUsage"],
        "Relationships": ["Dependency", "Specialization", "Subsetting", "Redefinition"]
    }
    
    for category, types in categories.items():
        count = sum(all_types.get(t, 0) for t in types)
        if count > 0:
            print(f"   • {category}: {count}")

def display_mermaid_diagram(project_id):
    """生成Mermaid图表代码"""
    print("\n" + "="*80)
    print("=== MERMAID DIAGRAM - SYSTEM ARCHITECTURE ===")
    print("="*80)
    print("\n```mermaid")
    print("graph TD")
    
    commits = get_project_commits(project_id)
    
    # 收集所有元素
    all_elements = {}
    requirements = []
    parts = []
    connections = []
    satisfies = []
    
    for commit in commits:
        elements = get_commit_elements(project_id, commit["@id"])
        for elem in elements:
            elem_id = elem.get("@id")
            elem_type = elem.get("@type")
            elem_name = elem.get("name", "unnamed")
            
            if elem_id:
                all_elements[elem_id] = {
                    "type": elem_type,
                    "name": elem_name
                }
                
                if "Requirement" in elem_type:
                    requirements.append(elem_id)
                elif "Part" in elem_type:
                    parts.append(elem_id)
                elif "Connection" in elem_type or "Connector" in elem_type:
                    connections.append(elem_id)
                elif "Satisfy" in elem_type:
                    satisfies.append(elem_id)
    
    # 生成节点
    print("    %% Requirements")
    for req_id in requirements[:5]:
        elem = all_elements[req_id]
        print(f"    R_{req_id[:8]}[{elem['name']}]")
    
    print("\n    %% Parts")
    for part_id in parts[:8]:
        elem = all_elements[part_id]
        print(f"    P_{part_id[:8]}[{elem['name']}]")
    
    # 生成连接
    if connections:
        print("\n    %% Connections")
        for i, conn_id in enumerate(connections[:5]):
            if i < len(parts) - 1:
                print(f"    P_{parts[i][:8]} --> P_{parts[i+1][:8]}")
    
    # 生成满足关系
    if satisfies and requirements and parts:
        print("\n    %% Satisfy Relationships")
        for i in range(min(3, len(requirements), len(parts))):
            print(f"    P_{parts[i][:8]} -.->|satisfies| R_{requirements[i][:8]}")
    
    print("```")

def display_plantuml_diagram(project_id):
    """生成PlantUML图表代码"""
    print("\n" + "="*80)
    print("=== PLANTUML DIAGRAM - REQUIREMENT DIAGRAM ===")
    print("="*80)
    print("\n```plantuml")
    print("@startuml")
    print("!define REQUIREMENT_COLOR #FFAAAA")
    print("!define PART_COLOR #AAAAFF")
    print("!define PORT_COLOR #AAFFAA")
    print("")
    
    commits = get_project_commits(project_id)
    
    # 收集元素
    requirements = []
    parts = []
    ports = []
    
    for commit in commits[:10]:  # 只处理前10个commits
        elements = get_commit_elements(project_id, commit["@id"])
        for elem in elements:
            elem_type = elem.get("@type")
            elem_name = elem.get("name", "unnamed")
            
            if "RequirementDefinition" in elem_type:
                requirements.append(elem_name)
            elif "PartDefinition" in elem_type:
                parts.append(elem_name)
            elif "PortDefinition" in elem_type:
                ports.append(elem_name)
    
    # 生成包
    print("package \"Requirements\" <<Rectangle>> REQUIREMENT_COLOR {")
    for req in requirements[:5]:
        print(f"  class \"{req}\" <<requirement>>")
    print("}")
    print("")
    
    print("package \"System Parts\" <<Rectangle>> PART_COLOR {")
    for part in parts[:5]:
        print(f"  class \"{part}\" <<part>>")
    print("}")
    print("")
    
    if ports:
        print("package \"Ports\" <<Rectangle>> PORT_COLOR {")
        for port in ports[:3]:
            print(f"  class \"{port}\" <<port>>")
        print("}")
        print("")
    
    # 生成关系
    if requirements and parts:
        for i in range(min(3, len(requirements), len(parts))):
            print(f"\"{parts[i]}\" ..> \"{requirements[i]}\" : satisfies")
    
    print("@enduml")
    print("```")

def display_csv_export(project_id):
    """CSV格式导出"""
    print("\n" + "="*80)
    print("=== CSV EXPORT - ELEMENT LIST ===")
    print("="*80)
    
    commits = get_project_commits(project_id)
    
    print("\nID,Type,Name,CommitID,Created")
    
    count = 0
    for commit in commits:
        commit_id = commit.get("@id")
        created = commit.get("created", "")
        elements = get_commit_elements(project_id, commit_id)
        
        for elem in elements:
            elem_id = elem.get("@id", "")
            elem_type = elem.get("@type", "")
            elem_name = elem.get("name", "")
            
            print(f"{elem_id},{elem_type},{elem_name},{commit_id},{created}")
            
            count += 1
            if count >= 20:  # 只显示前20条
                print("...")
                print(f"[Total {len(commits)} commits with elements]")
                return

def main():
    # 使用刚创建的完整制动系统项目
    project_id = "ca54e418-f2e2-462b-b097-293358195138"
    
    # 获取项目信息
    response = requests.get(f"{BASE_URL}/projects/{project_id}", headers=headers)
    if response.status_code != 200:
        print(f"Project not found: {project_id}")
        return
    
    brake_project = response.json()
    project_name = brake_project.get("name", "Unknown")
    
    print(f"🚗 Displaying Brake System Model: {project_name}")
    print(f"   Project ID: {project_id}")
    
    # 1. JSON格式展示（示例）
    commits = get_project_commits(project_id)
    if commits:
        first_commit = commits[0]
        elements = get_commit_elements(project_id, first_commit["@id"])
        if elements:
            display_json_format(elements[:2], "JSON Format (First 2 Elements)")
    
    # 2. 表格格式展示
    all_elements = []
    for commit in commits:
        elements = get_commit_elements(project_id, commit["@id"])
        all_elements.extend(elements)
    display_table_format(all_elements)
    
    # 3. 树形结构展示
    display_tree_format(project_id)
    
    # 4. 统计信息展示
    display_statistics(project_id)
    
    # 5. Mermaid图表
    display_mermaid_diagram(project_id)
    
    # 6. PlantUML图表
    display_plantuml_diagram(project_id)
    
    # 7. CSV导出格式
    display_csv_export(project_id)
    
    print("\n" + "="*80)
    print("✅ All visualization formats generated successfully!")
    print("="*80)

if __name__ == "__main__":
    main()