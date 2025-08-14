#!/usr/bin/env python3
import requests
import json
import uuid

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/ld+json"}

# 测试最简单的项目创建
print("Testing minimal project creation...")
project_body = {
    "@type": "Project"
}

print("Request body:")
print(json.dumps(project_body, indent=2))

response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
print(f"\nResponse status: {response.status_code}")
print(f"Response body: {response.text}")

# 如果失败，尝试带名称
if response.status_code != 200:
    print("\n\nTrying with name...")
    project_body = {
        "@type": "Project",
        "name": "Test Project"
    }
    
    response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
    print(f"Response status: {response.status_code}")
    print(f"Response body: {response.text}")

# 测试获取所有项目
print("\n\nTrying to get all projects...")
response = requests.get(f"{BASE_URL}/projects", headers=headers)
print(f"Response status: {response.status_code}")
print(f"Response body: {response.text[:500] if response.status_code == 200 else response.text}")