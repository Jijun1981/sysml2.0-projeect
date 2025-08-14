#!/usr/bin/env python3
import requests
import json
import uuid

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/ld+json"}

# 先创建一个新项目
project_body = {
    "@type": "Project",
    "name": f"Debug Project {uuid.uuid4().hex[:8]}"
}

response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
if response.status_code != 200:
    print(f"Failed to create project: {response.status_code}")
    print(response.text)
    exit(1)

project = response.json()
project_id = project["@id"]
print(f"Created project: {project_id}")

# 获取项目的默认分支
response = requests.get(f"{BASE_URL}/projects/{project_id}", headers=headers)
project_data = response.json()
default_branch_id = project_data.get("defaultBranch", {}).get("@id")
print(f"Default branch: {default_branch_id}")

# 尝试创建RequirementDefinition
print("\nAttempting to create RequirementDefinition...")
commit_body = {
    "@type": "Commit",
    "change": [{
        "@type": "DataVersion",
        "payload": {
            "@type": "RequirementDefinition",
            "name": "TestRequirement",
            "@id": str(uuid.uuid4())
        }
    }]
}

print("Request body:")
print(json.dumps(commit_body, indent=2))

response = requests.post(
    f"{BASE_URL}/projects/{project_id}/commits",
    json=commit_body,
    headers=headers
)

print(f"\nResponse status: {response.status_code}")
print(f"Response headers: {dict(response.headers)}")
print(f"Response body: {response.text}")

# 如果是500错误，尝试获取更多信息
if response.status_code == 500:
    print("\n\n=== Analyzing 500 Error ===")
    
    # 尝试创建一个简单的PartDefinition作为对比
    print("\nTrying PartDefinition for comparison...")
    commit_body2 = {
        "@type": "Commit",
        "change": [{
            "@type": "DataVersion",
            "payload": {
                "@type": "PartDefinition",
                "name": "TestPart",
                "@id": str(uuid.uuid4())
            }
        }]
    }
    
    response2 = requests.post(
        f"{BASE_URL}/projects/{project_id}/commits",
        json=commit_body2,
        headers=headers
    )
    print(f"PartDefinition response: {response2.status_code}")
    if response2.status_code == 200:
        print("PartDefinition created successfully!")
        print(response2.json())