#!/usr/bin/env python3
"""
SysML v2 汽车制动系统验证项目 - API客户端测试
基于官方SysML-v2-API-Services的REST API调用
"""

import requests
import json
import uuid
from datetime import datetime

class SysMLAPIClient:
    def __init__(self, base_url="http://localhost:9000"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
    
    def health_check(self):
        """检查API服务健康状态"""
        try:
            # 尝试多个可能的健康检查端点
            endpoints = ["/health", "/api/health", "/ping", "/"]
            for endpoint in endpoints:
                try:
                    response = self.session.get(f"{self.base_url}{endpoint}", timeout=5)
                    print(f"Testing {endpoint}: {response.status_code}")
                    if response.status_code == 200:
                        return True, endpoint
                except Exception as e:
                    print(f"Error testing {endpoint}: {e}")
                    continue
            return False, None
        except Exception as e:
            print(f"Health check failed: {e}")
            return False, None
    
    def get_projects(self):
        """获取所有项目"""
        response = self.session.get(f"{self.base_url}/projects")
        if response.status_code == 200:
            return response.json()
        return None
    
    def create_project(self, name, description=""):
        """创建新项目"""
        project_data = {
            "@type": "Project",
            "name": name,
            "description": description
        }
        response = self.session.post(f"{self.base_url}/projects", 
                                   data=json.dumps(project_data))
        if response.status_code == 200:
            return response.json()
        return None
    
    def create_commit(self, project_id, changes, comment=""):
        """创建提交"""
        commit_data = {
            "@type": "Commit",
            "change": changes,
            "comment": comment
        }
        response = self.session.post(f"{self.base_url}/projects/{project_id}/commits",
                                   data=json.dumps(commit_data))
        if response.status_code == 200:
            return response.json()
        return None
    
    def get_elements(self, project_id, commit_id):
        """获取指定提交的所有元素"""
        response = self.session.get(f"{self.base_url}/projects/{project_id}/commits/{commit_id}/elements")
        if response.status_code == 200:
            return response.json()
        return None

def create_braking_system_model():
    """创建汽车制动系统模型"""
    print("🚗 开始创建汽车制动系统SysML模型...")
    
    # 初始化API客户端
    client = SysMLAPIClient()
    
    # 1. 健康检查
    print("\n1. 检查API服务状态...")
    if not client.health_check():
        print("❌ API服务未运行，请先启动SysML-v2-API-Services")
        print("   启动命令: cd SysML-v2-API-Services && sbt run")
        return
    print("✅ API服务运行正常")
    
    # 2. 创建项目
    print("\n2. 创建制动系统验证项目...")
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    project_name = f"BrakingSystemValidation_{timestamp}"
    project = client.create_project(
        project_name, 
        "汽车制动系统SysML v2验证项目 - 包含需求、结构和验证模型"
    )
    
    if not project:
        print("❌ 项目创建失败")
        return
    
    project_id = project["@id"]
    print(f"✅ 项目创建成功: {project_name} (ID: {project_id})")
    
    # 3. 创建功能需求
    print("\n3. 创建制动系统功能需求...")
    
    # 制动距离需求
    braking_distance_req = {
        "@type": "DataVersion",
        "payload": {
            "@type": "RequirementDefinition",
            "declaredName": "BrakingDistanceRequirement",
            "text": "车辆必须在100km/h速度下40米内完全停止",
            "reqId": "REQ-FUNC-001"
        }
    }
    
    # 响应时间需求
    response_time_req = {
        "@type": "DataVersion", 
        "payload": {
            "@type": "RequirementDefinition",
            "declaredName": "ResponseTimeRequirement",
            "text": "制动系统响应时间不得超过150毫秒",
            "reqId": "REQ-FUNC-002"
        }
    }
    
    # 创建包含需求的提交
    commit1 = client.create_commit(
        project_id,
        [braking_distance_req, response_time_req],
        "添加制动系统功能需求"
    )
    
    if commit1:
        commit1_id = commit1["@id"]
        print(f"✅ 功能需求创建成功 (提交ID: {commit1_id})")
        
        # 获取创建的元素
        elements = client.get_elements(project_id, commit1_id)
        if elements:
            print(f"   共创建 {len(elements)} 个需求元素:")
            for elem in elements:
                print(f"   - {elem.get('declaredName', elem.get('name', 'Unknown'))}")
    else:
        print("❌ 功能需求创建失败")
        return
    
    # 4. 创建结构模型
    print("\n4. 创建制动系统结构模型...")
    
    # 制动系统主系统
    braking_system = {
        "@type": "DataVersion",
        "payload": {
            "@type": "PartDefinition", 
            "declaredName": "BrakingSystem",
            "text": "汽车制动系统主系统"
        }
    }
    
    # 制动踏板
    brake_pedal = {
        "@type": "DataVersion",
        "payload": {
            "@type": "PartDefinition",
            "declaredName": "BrakePedal", 
            "text": "制动踏板组件"
        }
    }
    
    # 制动主缸
    master_cylinder = {
        "@type": "DataVersion",
        "payload": {
            "@type": "PartDefinition",
            "declaredName": "MasterCylinder",
            "text": "制动主缸组件"
        }
    }
    
    # ABS控制器
    abs_controller = {
        "@type": "DataVersion", 
        "payload": {
            "@type": "PartDefinition",
            "declaredName": "ABSController",
            "text": "ABS防抱死控制系统"
        }
    }
    
    # 创建包含结构的提交
    commit2 = client.create_commit(
        project_id,
        [braking_system, brake_pedal, master_cylinder, abs_controller],
        "添加制动系统结构组件"
    )
    
    if commit2:
        commit2_id = commit2["@id"]
        print(f"✅ 结构模型创建成功 (提交ID: {commit2_id})")
        
        # 获取所有元素
        all_elements = client.get_elements(project_id, commit2_id)
        if all_elements:
            requirements = [e for e in all_elements if e.get('@type') == 'RequirementDefinition']
            parts = [e for e in all_elements if e.get('@type') == 'PartDefinition']
            
            print(f"   项目总结:")
            print(f"   - 需求定义: {len(requirements)} 个")
            print(f"   - 部件定义: {len(parts)} 个")
            print(f"   - 总元素数: {len(all_elements)} 个")
    else:
        print("❌ 结构模型创建失败")
        return
    
    print(f"\n🎉 汽车制动系统SysML模型创建完成!")
    print(f"📊 项目信息:")
    print(f"   项目名称: {project_name}")
    print(f"   项目ID: {project_id}")
    print(f"   最新提交: {commit2_id}")
    print(f"\n🔗 可通过以下API查看模型:")
    print(f"   项目: GET {client.base_url}/projects/{project_id}")
    print(f"   元素: GET {client.base_url}/projects/{project_id}/commits/{commit2_id}/elements")

if __name__ == "__main__":
    create_braking_system_model()