#!/usr/bin/env python3
"""
使用正确的API格式重新创建制动系统模型
"""

import requests
import json
from datetime import datetime

class BrakeSystemCreator:
    def __init__(self):
        self.base_url = "http://localhost:9000"
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.project_id = None
        self.created_elements = {
            'parts': [],
            'requirements': [],
            'usages': []
        }
    
    def create_project(self):
        """创建制动系统验证项目"""
        project_data = {
            "@type": "Project",
            "name": "BrakingSystemComplete",
            "description": "完整的制动系统模型 - 使用正确的API格式"
        }
        
        response = self.session.post(f"{self.base_url}/projects", json=project_data)
        if response.status_code == 200:
            project = response.json()
            self.project_id = project["@id"]
            print(f"✅ 项目创建成功: {project['name']}")
            return True
        return False
    
    def create_part_definitions(self):
        """创建所有部件定义"""
        print("\n=== 创建部件定义 (PartDefinition) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                # 系统级部件
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakingSystem",
                        "humanId": "PART-SYS-001",
                        "documentation": ["汽车制动系统总成"]
                    }
                },
                # 主要组件
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
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeDisc",
                        "humanId": "PART-COMP-003",
                        "documentation": ["制动盘"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeCaliper",
                        "humanId": "PART-COMP-004",
                        "documentation": ["制动卡钳"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "ABSController",
                        "humanId": "PART-COMP-005",
                        "documentation": ["ABS防抱死控制器"]
                    }
                },
                # 附加部件
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeFluid",
                        "humanId": "PART-FLUID-001",
                        "documentation": ["制动液"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakePads",
                        "humanId": "PART-COMP-006",
                        "documentation": ["制动片"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeRotor",
                        "humanId": "PART-COMP-007",
                        "documentation": ["制动转子"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakePipe",
                        "humanId": "PART-PIPE-001",
                        "documentation": ["制动管路"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "VacuumBooster",
                        "humanId": "PART-COMP-008",
                        "documentation": ["真空助力器"]
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "ElectronicStabilityControl",
                        "humanId": "PART-COMP-009",
                        "documentation": ["电子稳定控制系统"]
                    }
                }
            ]
        }
        
        response = self.session.post(
            f"{self.base_url}/projects/{self.project_id}/commits",
            json=commit_body
        )
        
        if response.status_code == 200:
            commit_id = response.json()["@id"]
            print(f"✅ 部件定义commit创建成功")
            
            # 获取创建的元素
            elements_response = self.session.get(
                f"{self.base_url}/projects/{self.project_id}/commits/{commit_id}/elements"
            )
            if elements_response.status_code == 200:
                elements = elements_response.json()
                print(f"✅ 成功创建 {len(elements)} 个部件定义")
                for elem in elements:
                    self.created_elements['parts'].append({
                        'name': elem['name'],
                        'id': elem['@id'],
                        'humanId': elem.get('humanId', '')
                    })
                return True
        return False
    
    def create_requirements(self):
        """创建需求定义"""
        print("\n=== 创建需求定义 (RequirementDefinition) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                # 功能需求
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
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "SafetyRequirement",
                        "humanId": "REQ-FUNC-003",
                        "text": "制动系统必须具备故障安全机制"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "EnvironmentalRequirement",
                        "humanId": "REQ-FUNC-004",
                        "text": "制动系统必须在-40°C至+85°C温度范围内正常工作"
                    }
                },
                # 性能需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "BrakingForceRequirement",
                        "humanId": "REQ-PERF-001",
                        "text": "制动力必须达到车重的0.8倍以上"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "DurabilityRequirement",
                        "humanId": "REQ-PERF-002",
                        "text": "制动系统必须承受10万次制动循环"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "TemperatureToleranceRequirement",
                        "humanId": "REQ-PERF-003",
                        "text": "制动系统必须在-40°C至+200°C范围内工作"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "WeightRequirement",
                        "humanId": "REQ-PERF-004",
                        "text": "制动系统总重量不得超过50kg"
                    }
                }
            ]
        }
        
        response = self.session.post(
            f"{self.base_url}/projects/{self.project_id}/commits",
            json=commit_body
        )
        
        if response.status_code == 200:
            commit_id = response.json()["@id"]
            print(f"✅ 需求定义commit创建成功")
            
            # 获取创建的元素
            elements_response = self.session.get(
                f"{self.base_url}/projects/{self.project_id}/commits/{commit_id}/elements"
            )
            if elements_response.status_code == 200:
                elements = elements_response.json()
                print(f"✅ 成功创建 {len(elements)} 个需求定义")
                for elem in elements:
                    self.created_elements['requirements'].append({
                        'name': elem['name'],
                        'id': elem['@id'],
                        'humanId': elem.get('humanId', '')
                    })
                return True
        return False
    
    def list_all_elements(self):
        """列出所有创建的元素"""
        print("\n" + "="*80)
        print("📊 制动系统模型元素汇总")
        print("="*80)
        
        # 部件列表
        print(f"\n🔧 部件定义 (PartDefinition) - 共 {len(self.created_elements['parts'])} 个:")
        print("-"*60)
        for part in self.created_elements['parts']:
            print(f"  {part['humanId']:<15} | {part['name']:<30}")
        
        # 需求列表
        print(f"\n📋 需求定义 (RequirementDefinition) - 共 {len(self.created_elements['requirements'])} 个:")
        print("-"*60)
        for req in self.created_elements['requirements']:
            print(f"  {req['humanId']:<15} | {req['name']:<30}")
        
        # 总计
        total = len(self.created_elements['parts']) + len(self.created_elements['requirements'])
        print(f"\n✅ 总计成功创建并存储 {total} 个模型元素")
    
    def run(self):
        """执行创建流程"""
        print("🚀 开始创建完整的制动系统模型")
        print("="*80)
        
        if self.create_project():
            if self.create_part_definitions():
                if self.create_requirements():
                    self.list_all_elements()
                    print("\n🎉 制动系统模型创建完成！所有数据已持久化到数据库。")
                else:
                    print("\n❌ 需求创建失败")
            else:
                print("\n❌ 部件创建失败")
        else:
            print("\n❌ 项目创建失败")

if __name__ == "__main__":
    creator = BrakeSystemCreator()
    creator.run()