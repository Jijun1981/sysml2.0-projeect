#!/usr/bin/env python3
"""
实现Phase3追踪矩阵中的W2到W4
W2: Requirements (CRUD)
W3: Parts + Ports (CRUD)  
W4: Connectors + Satisfy (CRUD)
"""
import requests
import json
import uuid
from datetime import datetime

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}  # 使用application/json避免JSON-LD问题

def create_project():
    """创建新项目"""
    project_body = {
        "@type": "Project",
        "name": f"BrakeSystem_Phase3_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
        "description": "Phase3 W2-W4 Brake System Implementation"
    }
    
    response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Failed to create project: {response.status_code} - {response.text}")
    
    project = response.json()
    return project["@id"]

def create_element(project_id, element_type, element_data):
    """创建元素的通用函数"""
    commit_body = {
        "@type": "Commit",
        "change": [{
            "@type": "DataVersion",
            "payload": {
                "@type": element_type,
                "@id": str(uuid.uuid4()),
                **element_data
            }
        }]
    }
    
    response = requests.post(
        f"{BASE_URL}/projects/{project_id}/commits",
        json=commit_body,
        headers=headers
    )
    
    if response.status_code != 200:
        print(f"Failed to create {element_type}: {response.status_code}")
        print(f"Response: {response.text}")
        return None
    
    commit = response.json()
    # API返回的是commit对象，元素ID是我们在请求中设置的
    element_id = commit_body["change"][0]["payload"]["@id"]
    print(f"Created {element_type}: {element_data.get('name', 'unnamed')} ({element_id})")
    return element_id

def main():
    try:
        # 创建项目
        project_id = create_project()
        print(f"Created project: {project_id}")
        
        # W2: Requirements (需求)
        print("\n=== W2: Creating Requirements ===")
        
        # W2-REQ-001: 制动性能需求
        req1_id = create_element(project_id, "RequirementDefinition", {
            "name": "BrakingPerformanceReq",
            "text": ["The brake system shall stop the vehicle from 100 km/h within 40 meters"],
            "reqId": "REQ-001"
        })
        
        # W2-REQ-002: 安全需求
        req2_id = create_element(project_id, "RequirementDefinition", {
            "name": "SafetyReq", 
            "text": ["The brake system shall maintain functionality even with single component failure"],
            "reqId": "REQ-002"
        })
        
        # W2-REQ-003: 响应时间需求
        req3_id = create_element(project_id, "RequirementDefinition", {
            "name": "ResponseTimeReq",
            "text": ["The brake system shall engage within 150ms of pedal press"],
            "reqId": "REQ-003"
        })
        
        # W3: Parts + Ports (部件和端口)
        print("\n=== W3: Creating Parts and Ports ===")
        
        # W3-PART-001: 制动系统总成
        brake_system_id = create_element(project_id, "PartDefinition", {
            "name": "BrakeSystem",
            "shortName": "BS"
        })
        
        # W3-PART-002: 制动踏板
        brake_pedal_id = create_element(project_id, "PartDefinition", {
            "name": "BrakePedal",
            "shortName": "BP"
        })
        
        # W3-PART-003: 主缸
        master_cylinder_id = create_element(project_id, "PartDefinition", {
            "name": "MasterCylinder",
            "shortName": "MC"
        })
        
        # W3-PART-004: 制动卡钳
        brake_caliper_id = create_element(project_id, "PartDefinition", {
            "name": "BrakeCaliper",
            "shortName": "BC"
        })
        
        # W3-PORT-001: 踏板输入端口
        pedal_input_port_id = create_element(project_id, "PortDefinition", {
            "name": "PedalInputPort"
        })
        
        # W3-PORT-002: 液压输出端口
        hydraulic_output_port_id = create_element(project_id, "PortDefinition", {
            "name": "HydraulicOutputPort"
        })
        
        # W4: Connectors + Satisfy (连接器和满足关系)
        print("\n=== W4: Creating Connectors and Satisfy Relationships ===")
        
        # W4-CONN-001: 踏板到主缸的连接
        conn1_id = create_element(project_id, "ConnectorAsUsage", {
            "name": "PedalToMasterCylinderConnection"
        })
        
        # W4-CONN-002: 主缸到卡钳的连接
        conn2_id = create_element(project_id, "ConnectorAsUsage", {
            "name": "MasterCylinderToCaliperConnection"
        })
        
        # W4-SAT-001: 制动系统满足性能需求
        if req1_id and brake_system_id:
            sat1_id = create_element(project_id, "SatisfyRequirementUsage", {
                "name": "BrakeSystemSatisfiesPerformance"
            })
        
        # W4-SAT-002: 系统满足安全需求
        if req2_id and brake_system_id:
            sat2_id = create_element(project_id, "SatisfyRequirementUsage", {
                "name": "BrakeSystemSatisfiesSafety"
            })
        
        # W4-SAT-003: 踏板满足响应时间需求
        if req3_id and brake_pedal_id:
            sat3_id = create_element(project_id, "SatisfyRequirementUsage", {
                "name": "BrakePedalSatisfiesResponseTime"
            })
        
        print("\n=== Phase3 W2-W4 Implementation Complete ===")
        print(f"Project ID: {project_id}")
        
        # 查询创建的元素
        print("\n=== Verifying Created Elements ===")
        response = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits",
            headers=headers
        )
        
        if response.status_code == 200:
            commits = response.json()
            print(f"Total commits: {len(commits)}")
            
            # 获取最新的commit
            if commits:
                latest_commit = commits[0]
                commit_id = latest_commit["@id"]
                
                # 获取该commit的所有元素
                response = requests.get(
                    f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/elements",
                    headers=headers
                )
                
                if response.status_code == 200:
                    elements = response.json()
                    print(f"Total elements in latest commit: {len(elements)}")
                    
                    # 按类型统计
                    type_counts = {}
                    for elem in elements:
                        elem_type = elem.get("@type", "Unknown")
                        type_counts[elem_type] = type_counts.get(elem_type, 0) + 1
                    
                    print("\nElement counts by type:")
                    for elem_type, count in sorted(type_counts.items()):
                        print(f"  {elem_type}: {count}")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()