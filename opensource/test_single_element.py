#!/usr/bin/env python3
import requests
import json
import uuid

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}

# 创建项目
project_body = {
    "@type": "Project",
    "name": "Test Single Element"
}

response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
if response.status_code != 200:
    print(f"Failed to create project: {response.status_code}")
    exit(1)

project = response.json()
project_id = project["@id"]
print(f"Created project: {project_id}")

# 测试创建PartDefinition
print("\nTesting PartDefinition creation...")
commit_body = {
    "@type": "Commit",
    "change": [{
        "@type": "DataVersion",
        "payload": {
            "@type": "PartDefinition",
            "@id": str(uuid.uuid4()),
            "name": "TestPart"
        }
    }]
}

print("Request:")
print(json.dumps(commit_body, indent=2))

response = requests.post(
    f"{BASE_URL}/projects/{project_id}/commits",
    json=commit_body,
    headers=headers
)

print(f"\nResponse status: {response.status_code}")
print("Response body:")
print(json.dumps(response.json(), indent=2))