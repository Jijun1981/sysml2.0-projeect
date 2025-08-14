#!/usr/bin/env python3
"""
检查数据是否真正存储在数据库中
"""
import requests
import json

BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}

# 使用刚创建的项目ID
project_id = "ca54e418-f2e2-462b-b097-293358195138"

print("=" * 80)
print("检查项目数据是否真正存储在数据库中")
print("=" * 80)

# 1. 检查项目是否存在
print(f"\n1. 检查项目 {project_id}")
response = requests.get(f"{BASE_URL}/projects/{project_id}", headers=headers)
print(f"   Status: {response.status_code}")
if response.status_code == 200:
    project = response.json()
    print(f"   Name: {project.get('name')}")
    print(f"   Created: {project.get('created')}")
else:
    print(f"   ERROR: {response.text}")

# 2. 获取所有commits
print(f"\n2. 获取项目的所有commits")
response = requests.get(f"{BASE_URL}/projects/{project_id}/commits", headers=headers)
print(f"   Status: {response.status_code}")
if response.status_code == 200:
    commits = response.json()
    print(f"   Total Commits: {len(commits)}")
    
    # 3. 检查每个commit的元素
    print(f"\n3. 检查每个commit中的元素")
    total_elements = 0
    element_types = {}
    
    for i, commit in enumerate(commits[:10]):  # 只检查前10个
        commit_id = commit["@id"]
        response = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/elements",
            headers=headers
        )
        
        if response.status_code == 200:
            elements = response.json()
            total_elements += len(elements)
            
            print(f"\n   Commit {i+1} ({commit_id[:8]}...):")
            print(f"     Elements: {len(elements)}")
            
            for elem in elements:
                elem_type = elem.get("@type", "Unknown")
                elem_name = elem.get("name", "unnamed")
                elem_id = elem.get("@id", "no-id")
                
                element_types[elem_type] = element_types.get(elem_type, 0) + 1
                
                if i < 3:  # 只显示前3个commit的详细信息
                    print(f"       - {elem_type}: {elem_name} ({elem_id[:8]}...)")
        else:
            print(f"   Commit {i+1}: ERROR {response.status_code}")
    
    if len(commits) > 10:
        print(f"\n   ... and {len(commits) - 10} more commits")
    
    # 4. 统计汇总
    print(f"\n4. 数据统计汇总")
    print(f"   Total Elements Found: {total_elements}")
    print(f"   Element Types:")
    for elem_type, count in sorted(element_types.items(), key=lambda x: x[1], reverse=True):
        print(f"     - {elem_type}: {count}")
    
    # 5. 测试数据持久性 - 获取特定元素
    if commits:
        print(f"\n5. 测试数据持久性")
        first_commit = commits[0]
        commit_id = first_commit["@id"]
        
        # 获取该commit的change
        response = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/changes",
            headers=headers
        )
        print(f"   获取commit changes: {response.status_code}")
        
        if response.status_code == 200:
            changes = response.json()
            print(f"   Changes found: {len(changes)}")
            for change in changes[:3]:
                print(f"     - Change ID: {change.get('@id', 'unknown')}")
        
else:
    print(f"   ERROR: {response.text}")

print("\n" + "=" * 80)
print("检查完成！")
print("=" * 80)