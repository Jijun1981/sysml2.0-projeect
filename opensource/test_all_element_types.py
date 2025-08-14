#!/usr/bin/env python3
"""
测试所有SysML元素类型创建
"""
import requests
import json
import uuid
from datetime import datetime

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}

def create_project():
    """创建新项目"""
    project_body = {
        "@type": "Project",
        "name": f"TestAllTypes_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    }
    
    response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Failed to create project: {response.status_code}")
    
    return response.json()["@id"]

def test_element_type(project_id, element_type, additional_props=None):
    """测试创建特定类型的元素"""
    props = {"name": f"Test{element_type}"}
    if additional_props:
        props.update(additional_props)
    
    commit_body = {
        "@type": "Commit", 
        "change": [{
            "@type": "DataVersion",
            "payload": {
                "@type": element_type,
                "@id": str(uuid.uuid4()),
                **props
            }
        }]
    }
    
    response = requests.post(
        f"{BASE_URL}/projects/{project_id}/commits",
        json=commit_body,
        headers=headers
    )
    
    return response.status_code == 200, response.status_code, response.text

def main():
    project_id = create_project()
    print(f"Created project: {project_id}\n")
    
    # 测试各种元素类型
    test_types = [
        # W2: Requirements
        ("RequirementDefinition", {"reqId": "REQ-001", "text": ["Test requirement"]}),
        ("RequirementUsage", None),
        ("RequirementConstraintMembership", None),
        ("RequirementVerificationMembership", None),
        
        # W3: Parts and Ports
        ("PartDefinition", None),
        ("PartUsage", None),
        ("PortDefinition", None),
        ("PortUsage", None),
        ("InterfaceDefinition", None),
        ("InterfaceUsage", None),
        
        # W4: Connections and Relationships
        ("ConnectionDefinition", None),
        ("ConnectionUsage", None),
        ("Connector", None),
        ("BindingConnector", None),
        ("BindingConnectorAsUsage", None),
        # ("ConnectorAsUsage", None),  # 已知是abstract，会失败
        
        # Satisfy relationships
        ("SatisfyRequirementUsage", None),
        
        # Actions
        ("ActionDefinition", None),
        ("ActionUsage", None),
        
        # States
        ("StateDefinition", None),
        ("StateUsage", None),
        
        # Constraints
        ("ConstraintDefinition", None),
        ("ConstraintUsage", None),
        
        # Use Cases
        ("UseCaseDefinition", None),
        ("UseCaseUsage", None),
        
        # Attributes
        ("AttributeDefinition", None),
        ("AttributeUsage", None),
        
        # Items
        ("ItemDefinition", None),
        ("ItemUsage", None),
        
        # Allocations
        ("AllocationDefinition", None),
        ("AllocationUsage", None),
        
        # Analysis and Verification
        ("AnalysisCaseDefinition", None),
        ("AnalysisCaseUsage", None),
        ("VerificationCaseDefinition", None),
        ("VerificationCaseUsage", None),
        
        # Views
        ("ViewDefinition", None),
        ("ViewUsage", None),
        ("ViewpointDefinition", None),
        ("ViewpointUsage", None),
        
        # Packages and Namespaces
        ("Package", None),
        
        # Relationships
        ("Dependency", None),
        ("Specialization", None),
        ("Subsetting", None),
        ("Redefinition", None),
        ("FeatureMembership", None),
        ("OwningMembership", None),
    ]
    
    results = []
    for element_type, props in test_types:
        success, status_code, error = test_element_type(project_id, element_type, props)
        results.append((element_type, success, status_code))
        
        if success:
            print(f"✅ {element_type}: SUCCESS")
        else:
            print(f"❌ {element_type}: FAILED ({status_code})")
            if "InvalidTypeIdException" in error:
                print(f"   -> Type not recognized by Jackson")
            elif "abstract" in error.lower():
                print(f"   -> Abstract class")
            else:
                # 只打印错误的前100个字符
                print(f"   -> {error[:100]}...")
    
    # 统计结果
    print("\n=== SUMMARY ===")
    success_count = sum(1 for _, success, _ in results if success)
    total_count = len(results)
    print(f"Success: {success_count}/{total_count} ({success_count*100//total_count}%)")
    
    print("\n=== SUCCESSFUL TYPES ===")
    for element_type, success, _ in results:
        if success:
            print(f"  - {element_type}")
    
    print("\n=== FAILED TYPES ===")
    for element_type, success, status_code in results:
        if not success:
            print(f"  - {element_type} ({status_code})")

if __name__ == "__main__":
    main()