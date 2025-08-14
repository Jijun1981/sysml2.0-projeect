#!/usr/bin/env python3
"""
测试SysML v2 API元素创建和检索
验证数据是否真实存储在数据库中
"""

import requests
import json
from datetime import datetime

class SysMLElementTester:
    def __init__(self):
        self.base_url = "http://localhost:9000"
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.project_id = None
        
    def create_test_project(self):
        """创建测试项目"""
        timestamp = datetime.now()
        project_name = f"Element Retrieval Test {timestamp}"
        project_data = {
            "@type": "Project",
            "name": project_name,
            "description": "Testing element creation and retrieval"
        }
        
        response = self.session.post(f"{self.base_url}/projects", json=project_data)
        if response.status_code == 200:
            project = response.json()
            self.project_id = project["@id"]
            print(f"✅ 项目创建成功: {project_name}")
            print(f"   项目ID: {self.project_id}")
            return True
        return False
    
    def create_elements_correctly(self):
        """使用正确的格式创建元素"""
        print("\n=== 创建制动系统元素（正确格式）===")
        
        # 按照Cookbook的格式，使用DataVersion包装
        commit_body = {
            "@type": "Commit",
            "change": [
                # 需求定义
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "BrakingDistanceRequirement",
                        "humanId": "REQ-FUNC-001",
                        "text": "车辆必须在100km/h速度下40米内完全停止"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "ResponseTimeRequirement",
                        "humanId": "REQ-FUNC-002",
                        "text": "制动系统响应时间不得超过150毫秒"
                    }
                },
                # 部件定义
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakingSystem",
                        "humanId": "PART-SYS-001",
                        "documentation": ["汽车制动系统总成"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakePedal",
                        "humanId": "PART-COMP-001",
                        "documentation": ["制动踏板组件"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "MasterCylinder",
                        "humanId": "PART-COMP-002",
                        "documentation": ["制动主缸"]
                    }
                }
            ]
        }
        
        # 创建commit
        response = self.session.post(
            f"{self.base_url}/projects/{self.project_id}/commits",
            json=commit_body
        )
        
        if response.status_code == 200:
            commit = response.json()
            commit_id = commit["@id"]
            print(f"✅ Commit创建成功: {commit_id}")
            
            # 立即获取元素
            elements_url = f"{self.base_url}/projects/{self.project_id}/commits/{commit_id}/elements"
            elements_response = self.session.get(elements_url)
            
            if elements_response.status_code == 200:
                elements = elements_response.json()
                print(f"\n✅ 成功获取 {len(elements)} 个元素:")
                
                # 分类显示
                requirements = []
                parts = []
                
                for elem in elements:
                    elem_type = elem.get("@type", "Unknown")
                    name = elem.get("name", "无名称")
                    elem_id = elem.get("@id", "N/A")
                    human_id = elem.get("humanId", "N/A")
                    
                    if "Requirement" in elem_type:
                        requirements.append((name, human_id, elem_id))
                    elif "Part" in elem_type:
                        parts.append((name, human_id, elem_id))
                
                print(f"\n📋 需求定义 ({len(requirements)}个):")
                for name, human_id, elem_id in requirements:
                    print(f"   - {name} ({human_id})")
                    print(f"     ID: {elem_id}")
                
                print(f"\n🔧 部件定义 ({len(parts)}个):")
                for name, human_id, elem_id in parts:
                    print(f"   - {name} ({human_id})")
                    print(f"     ID: {elem_id}")
                
                return True
            else:
                print(f"❌ 获取元素失败: {elements_response.status_code}")
                return False
        else:
            print(f"❌ Commit创建失败: {response.status_code}")
            print(f"   错误: {response.text}")
            return False
    
    def verify_element_persistence(self):
        """验证元素是否持久化存储"""
        print("\n=== 验证数据持久化 ===")
        
        # 获取项目的所有commits
        commits_response = self.session.get(f"{self.base_url}/projects/{self.project_id}/commits")
        if commits_response.status_code == 200:
            commits = commits_response.json()
            print(f"项目包含 {len(commits)} 个commits")
            
            total_elements = 0
            for commit in commits:
                commit_id = commit["@id"]
                elements_url = f"{self.base_url}/projects/{self.project_id}/commits/{commit_id}/elements"
                elements_response = self.session.get(elements_url)
                
                if elements_response.status_code == 200:
                    elements = elements_response.json()
                    total_elements += len(elements)
                    if elements:
                        print(f"\nCommit {commit_id}:")
                        print(f"  包含 {len(elements)} 个元素")
            
            print(f"\n✅ 总计持久化存储了 {total_elements} 个元素")
            return total_elements > 0
        return False
    
    def run_test(self):
        """运行完整测试"""
        print("🚀 开始SysML v2 API元素创建和检索测试")
        print("=" * 60)
        
        if self.create_test_project():
            if self.create_elements_correctly():
                if self.verify_element_persistence():
                    print("\n✅ 测试成功！数据确实存储在数据库中并可以检索。")
                else:
                    print("\n❌ 数据持久化验证失败")
            else:
                print("\n❌ 元素创建失败")
        else:
            print("\n❌ 项目创建失败")

if __name__ == "__main__":
    tester = SysMLElementTester()
    tester.run_test()