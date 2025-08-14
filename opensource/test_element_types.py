#!/usr/bin/env python3
"""测试SysML v2 API支持的元素类型"""

import requests
import json

base_url = 'http://localhost:9000'
session = requests.Session()
session.headers.update({'Content-Type': 'application/json'})

# 使用现有项目
project_id = 'd5f10894-9aa5-4515-ba01-6007ee23eaad'

# 测试不同的元素类型
test_types = [
    # 官方可能支持的类型
    ('Package', 'TestPackage'),
    ('Class', 'TestClass'),
    ('Feature', 'TestFeature'),
    ('Type', 'TestType'),
    ('Element', 'TestElement'),
    # 简化的类型名
    ('Part', 'TestPart'),
    ('Port', 'TestPort'),
    ('Action', 'TestAction'),
    ('State', 'TestState'),
    ('Constraint', 'TestConstraint'),
    ('UseCase', 'TestUseCase'),
    ('Connection', 'TestConnection'),
    ('Interface', 'TestInterface'),
    # 不带Definition的版本
    ('Requirement', 'TestRequirement'),
]

print("=== 测试SysML v2 API支持的元素类型 ===\n")

for elem_type, elem_name in test_types:
    test_commit = {
        '@type': 'Commit',
        'change': [{
            '@type': 'DataVersion',
            'payload': {
                '@type': elem_type,
                'name': elem_name
            }
        }]
    }
    
    resp = session.post(f'{base_url}/projects/{project_id}/commits', json=test_commit)
    
    if resp.status_code == 200:
        print(f'✅ {elem_type:<20} - 成功')
        # 获取创建的元素
        commit_id = resp.json()['@id']
        elements = session.get(f'{base_url}/projects/{project_id}/commits/{commit_id}/elements').json()
        if elements:
            elem = elements[0]
            print(f'   创建的元素: {elem.get("name")} (类型: {elem.get("@type")})')
    else:
        print(f'❌ {elem_type:<20} - 失败 ({resp.status_code})')

print("\n注：看起来API只支持特定的元素类型，可能需要查看官方文档")