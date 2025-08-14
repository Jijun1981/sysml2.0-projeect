#!/usr/bin/env python3
"""
展示系统中各种ID的关系：Project ID, Commit ID, Element ID
"""
import requests
import json

BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}

def main():
    project_id = "ca54e418-f2e2-462b-b097-293358195138"
    
    print("=" * 80)
    print("SysML v2 API 系统中的ID体系")
    print("=" * 80)
    
    # 1. 获取项目信息
    response = requests.get(f"{BASE_URL}/projects/{project_id}", headers=headers)
    project = response.json()
    
    print(f"\n1. PROJECT层级")
    print(f"   Project ID: {project['@id']}")
    print(f"   Project Name: {project.get('name', 'unnamed')}")
    print(f"   Created: {project.get('created', 'unknown')}")
    
    # 2. 获取commits
    response = requests.get(f"{BASE_URL}/projects/{project_id}/commits", headers=headers)
    commits = response.json()
    
    print(f"\n2. COMMIT层级")
    print(f"   Total Commits: {len(commits)}")
    
    # 只展示前3个commits的详细信息
    for i, commit in enumerate(commits[:3]):
        commit_id = commit["@id"]
        print(f"\n   Commit #{i+1}:")
        print(f"     Commit ID: {commit_id}")
        print(f"     Created: {commit.get('created', 'unknown')}")
        
        # 获取该commit的elements
        response = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/elements",
            headers=headers
        )
        elements = response.json()
        
        print(f"     Elements in this commit: {len(elements)}")
        
        # 展示该commit中的元素
        for j, elem in enumerate(elements[:2]):  # 只显示前2个元素
            elem_id = elem.get("@id", "no-id")
            elem_type = elem.get("@type", "unknown")
            elem_name = elem.get("name", "unnamed")
            
            print(f"\n     Element #{j+1}:")
            print(f"       Element ID: {elem_id}")
            print(f"       Element Type: {elem_type}")
            print(f"       Element Name: {elem_name}")
            
            # 展示元素的其他ID相关字段
            if "qualifiedName" in elem:
                print(f"       Qualified Name: {elem['qualifiedName']}")
            if "declaredName" in elem:
                print(f"       Declared Name: {elem['declaredName']}")
            if "shortName" in elem:
                print(f"       Short Name: {elem['shortName']}")
            if "elementId" in elem:
                print(f"       ElementId field: {elem['elementId']}")
    
    # 3. 展示ID的唯一性分析
    print("\n" + "=" * 80)
    print("3. ID唯一性分析")
    print("=" * 80)
    
    all_element_ids = set()
    element_id_occurrences = {}
    
    for commit in commits:
        commit_id = commit["@id"]
        elements = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/elements",
            headers=headers
        ).json()
        
        for elem in elements:
            elem_id = elem.get("@id")
            if elem_id:
                if elem_id not in element_id_occurrences:
                    element_id_occurrences[elem_id] = []
                element_id_occurrences[elem_id].append({
                    "commit_id": commit_id,
                    "type": elem.get("@type"),
                    "name": elem.get("name")
                })
                all_element_ids.add(elem_id)
    
    print(f"\n   总共找到的唯一Element IDs: {len(all_element_ids)}")
    
    # 检查是否有重复的element ID在不同commits中
    duplicates = {k: v for k, v in element_id_occurrences.items() if len(v) > 1}
    
    if duplicates:
        print(f"\n   发现重复的Element IDs (在多个commits中出现):")
        for elem_id, occurrences in list(duplicates.items())[:3]:  # 只显示前3个
            print(f"\n   Element ID: {elem_id}")
            for occ in occurrences[:2]:  # 只显示前2次出现
                print(f"     - In Commit: {occ['commit_id'][:8]}...")
                print(f"       Type: {occ['type']}, Name: {occ['name']}")
    else:
        print(f"\n   ✓ 没有发现重复的Element IDs")
    
    # 4. 展示完整的ID层级关系
    print("\n" + "=" * 80)
    print("4. 完整的ID层级关系示例")
    print("=" * 80)
    
    # 选择一个具体的元素展示完整路径
    if commits and len(commits) > 0:
        sample_commit = commits[0]
        sample_commit_id = sample_commit["@id"]
        
        elements = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits/{sample_commit_id}/elements",
            headers=headers
        ).json()
        
        if elements:
            sample_element = elements[0]
            
            print(f"\n   访问路径:")
            print(f"   └── Project: {project_id}")
            print(f"       └── Commit: {sample_commit_id}")
            print(f"           └── Element: {sample_element.get('@id')}")
            
            print(f"\n   API调用链:")
            print(f"   1. GET /projects/{project_id}")
            print(f"   2. GET /projects/{project_id}/commits")
            print(f"   3. GET /projects/{project_id}/commits/{sample_commit_id}")
            print(f"   4. GET /projects/{project_id}/commits/{sample_commit_id}/elements")
            
            print(f"\n   元素详细信息:")
            print(f"   {json.dumps(sample_element, indent=6, ensure_ascii=False)[:500]}...")
    
    # 5. 统计各种ID的格式
    print("\n" + "=" * 80)
    print("5. ID格式分析")
    print("=" * 80)
    
    print(f"\n   Project ID格式: UUID v4")
    print(f"   示例: {project_id}")
    
    if commits:
        print(f"\n   Commit ID格式: UUID v4")
        print(f"   示例: {commits[0]['@id']}")
    
    if all_element_ids:
        sample_elem_id = list(all_element_ids)[0]
        print(f"\n   Element ID格式: UUID v4")
        print(f"   示例: {sample_elem_id}")
    
    print("\n" + "=" * 80)
    print("✅ ID体系分析完成!")
    print("=" * 80)

if __name__ == "__main__":
    main()