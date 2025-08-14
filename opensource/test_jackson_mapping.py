#!/usr/bin/env python3
import requests
import json

# API基础配置
BASE_URL = "http://localhost:9000"

# 测试不同的Content-Type
print("Testing different content types...")

# 测试1: application/json
headers1 = {"Content-Type": "application/json"}
body1 = {
    "@type": "Project",
    "name": "Test Project JSON"
}

print("\n=== Test 1: application/json ===")
print(f"Headers: {headers1}")
print(f"Body: {json.dumps(body1, indent=2)}")

response = requests.post(f"{BASE_URL}/projects", json=body1, headers=headers1)
print(f"Response: {response.status_code}")
print(f"Body: {response.text}")

# 测试2: application/ld+json
headers2 = {"Content-Type": "application/ld+json"}
body2 = {
    "@type": "Project",
    "name": "Test Project LD+JSON"
}

print("\n=== Test 2: application/ld+json ===")
print(f"Headers: {headers2}")
print(f"Body: {json.dumps(body2, indent=2)}")

response = requests.post(f"{BASE_URL}/projects", json=body2, headers=headers2)
print(f"Response: {response.status_code}")
print(f"Body: {response.text}")

# 测试3: 添加@context
headers3 = {"Content-Type": "application/ld+json"}
body3 = {
    "@context": "https://www.omg.org/spec/SysML/20250201",
    "@type": "Project",
    "name": "Test Project with Context"
}

print("\n=== Test 3: with @context ===")
print(f"Headers: {headers3}")
print(f"Body: {json.dumps(body3, indent=2)}")

response = requests.post(f"{BASE_URL}/projects", json=body3, headers=headers3)
print(f"Response: {response.status_code}")
print(f"Body: {response.text}")

# 测试4: 更完整的对象
headers4 = {"Content-Type": "application/ld+json"}
body4 = {
    "@type": "Project",
    "name": "Complete Project",
    "description": "A test project",
    "alias": ["test", "demo"]
}

print("\n=== Test 4: complete object ===")
print(f"Headers: {headers4}")
print(f"Body: {json.dumps(body4, indent=2)}")

response = requests.post(f"{BASE_URL}/projects", json=body4, headers=headers4)
print(f"Response: {response.status_code}")
print(f"Body: {response.text}")