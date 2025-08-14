#!/usr/bin/env python3
import requests
import json
import uuid
import time

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/ld+json"}

# 等待服务启动
print("Waiting for service to be ready...")
for i in range(30):
    try:
        response = requests.get(f"{BASE_URL}/projects", headers=headers)
        if response.status_code == 200:
            print("Service is ready!")
            break
    except:
        pass
    time.sleep(1)
else:
    print("Service not responding after 30 seconds")
    exit(1)

# 创建新项目
project_body = {
    "@type": "Project",
    "name": f"Minimal Test {uuid.uuid4().hex[:8]}"
}

response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
if response.status_code != 200:
    print(f"Failed to create project: {response.status_code}")
    print(response.text)
    exit(1)

project = response.json()
project_id = project["@id"]
print(f"Created project: {project_id}")

# 测试1：创建最简单的PartDefinition
print("\n=== Test 1: Minimal PartDefinition ===")
commit_body = {
    "@type": "Commit",
    "change": [{
        "@type": "DataVersion",
        "payload": {
            "@type": "PartDefinition"
        }
    }]
}

response = requests.post(
    f"{BASE_URL}/projects/{project_id}/commits",
    json=commit_body,
    headers=headers
)
print(f"Response: {response.status_code}")
if response.status_code == 200:
    print("Success!")
else:
    print(f"Error: {response.text}")

# 测试2：创建最简单的RequirementDefinition
print("\n=== Test 2: Minimal RequirementDefinition ===")
commit_body = {
    "@type": "Commit",
    "change": [{
        "@type": "DataVersion",
        "payload": {
            "@type": "RequirementDefinition"
        }
    }]
}

response = requests.post(
    f"{BASE_URL}/projects/{project_id}/commits",
    json=commit_body,
    headers=headers
)
print(f"Response: {response.status_code}")
if response.status_code == 200:
    print("Success!")
else:
    print(f"Error: {response.text}")

# 测试3：带名称的RequirementDefinition
print("\n=== Test 3: RequirementDefinition with name ===")
commit_body = {
    "@type": "Commit",
    "change": [{
        "@type": "DataVersion",
        "payload": {
            "@type": "RequirementDefinition",
            "name": "TestReq"
        }
    }]
}

response = requests.post(
    f"{BASE_URL}/projects/{project_id}/commits",
    json=commit_body,
    headers=headers
)
print(f"Response: {response.status_code}")
if response.status_code == 200:
    print("Success!")
else:
    print(f"Error: {response.text}")

# 测试4：带ID的RequirementDefinition
print("\n=== Test 4: RequirementDefinition with @id ===")
commit_body = {
    "@type": "Commit",
    "change": [{
        "@type": "DataVersion",
        "payload": {
            "@type": "RequirementDefinition",
            "@id": str(uuid.uuid4()),
            "name": "TestReq2"
        }
    }]
}

response = requests.post(
    f"{BASE_URL}/projects/{project_id}/commits",
    json=commit_body,
    headers=headers
)
print(f"Response: {response.status_code}")
if response.status_code == 200:
    print("Success!")
else:
    print(f"Error: {response.text}")