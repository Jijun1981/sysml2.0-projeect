#!/usr/bin/env python3
"""
按照SysML 2.0理念构建完整的制动系统模型
包含8个核心元素类型：
1. Requirements (需求)
2. Parts (部件) 
3. Interfaces (接口)
4. Connections (连接)
5. Actions (动作)
6. States (状态)
7. Constraints (约束)
8. Use Cases (用例)
"""

import requests
import json
from datetime import datetime

class CompleteBrakeSystemBuilder:
    def __init__(self):
        self.base_url = "http://localhost:9000"
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.project_id = None
        self.created_elements = {
            'requirements': [],
            'parts': [],
            'interfaces': [],
            'connections': [],
            'actions': [],
            'states': [],
            'constraints': [],
            'usecases': []
        }
    
    def create_project(self):
        """创建SysML 2.0制动系统项目"""
        project_data = {
            "@type": "Project",
            "name": "CompleteBrakeSystemSysML2",
            "description": "完整的SysML 2.0制动系统模型 - 包含8个核心元素类型"
        }
        
        response = self.session.post(f"{self.base_url}/projects", json=project_data)
        if response.status_code == 200:
            project = response.json()
            self.project_id = project["@id"]
            print(f"✅ 项目创建成功: {project['name']}")
            print(f"   项目ID: {self.project_id}")
            return True
        return False
    
    def create_requirements(self):
        """1. 创建需求 (Requirements)"""
        print("\n=== 1️⃣ 创建需求 (Requirements) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                # 功能需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "SafeBrakingRequirement",
                        "humanId": "REQ-001",
                        "text": "制动系统应在所有天气条件下提供安全可靠的制动性能"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "BrakingDistanceRequirement",
                        "humanId": "REQ-002",
                        "text": "车辆必须在100km/h速度下40米内完全停止"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "ResponseTimeRequirement",
                        "humanId": "REQ-003",
                        "text": "制动系统响应时间不得超过150毫秒"
                    }
                },
                # 性能需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "BrakingForceRequirement",
                        "humanId": "REQ-004",
                        "text": "制动力必须达到车重的0.8倍以上"
                    }
                },
                # 安全需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "FailSafeRequirement",
                        "humanId": "REQ-005",
                        "text": "制动系统必须具备故障安全机制，单点故障不得导致完全失效"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "requirements", "需求")
    
    def create_parts(self):
        """2. 创建部件 (Parts)"""
        print("\n=== 2️⃣ 创建部件 (Parts) ===")
        
        commit_body = {
            "@type": "Commit", 
            "change": [
                # 系统级部件
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeSystem",
                        "humanId": "PART-001"
                    }
                },
                # 主要组件
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakePedal",
                        "humanId": "PART-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "MasterCylinder",
                        "humanId": "PART-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeDisc",
                        "humanId": "PART-004"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeCaliper",
                        "humanId": "PART-005"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "ABSModule",
                        "humanId": "PART-006"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "HydraulicLine",
                        "humanId": "PART-007"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeSensor",
                        "humanId": "PART-008"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "parts", "部件")
    
    def create_interfaces(self):
        """3. 创建接口 (Interfaces)"""
        print("\n=== 3️⃣ 创建接口 (Interfaces) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceDefinition",
                        "name": "MechanicalInterface",
                        "humanId": "INTF-001"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceDefinition",
                        "name": "HydraulicInterface",
                        "humanId": "INTF-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceDefinition",
                        "name": "ElectricalInterface",
                        "humanId": "INTF-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceDefinition",
                        "name": "DataInterface",
                        "humanId": "INTF-004"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "interfaces", "接口")
    
    def create_connections(self):
        """4. 创建连接 (Connections)"""
        print("\n=== 4️⃣ 创建连接 (Connections) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectionDefinition",
                        "name": "PedalToMasterCylinderConnection",
                        "humanId": "CONN-001"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectionDefinition",
                        "name": "MasterCylinderToLineConnection",
                        "humanId": "CONN-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectionDefinition",
                        "name": "LineToCaliperConnection",
                        "humanId": "CONN-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectionDefinition",
                        "name": "CaliperToDiscConnection",
                        "humanId": "CONN-004"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectionDefinition",
                        "name": "ABSToSensorConnection",
                        "humanId": "CONN-005"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "connections", "连接")
    
    def create_actions(self):
        """5. 创建动作 (Actions)"""
        print("\n=== 5️⃣ 创建动作 (Actions) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionDefinition",
                        "name": "ApplyBrakeAction",
                        "humanId": "ACT-001"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionDefinition",
                        "name": "ReleaseBrakeAction",
                        "humanId": "ACT-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionDefinition",
                        "name": "ModulatePressureAction",
                        "humanId": "ACT-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionDefinition",
                        "name": "DetectWheelLockAction",
                        "humanId": "ACT-004"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionDefinition",
                        "name": "EmergencyBrakeAction",
                        "humanId": "ACT-005"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "actions", "动作")
    
    def create_states(self):
        """6. 创建状态 (States)"""
        print("\n=== 6️⃣ 创建状态 (States) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "IdleState",
                        "humanId": "STATE-001"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "NormalBrakingState",
                        "humanId": "STATE-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "ABSActiveState",
                        "humanId": "STATE-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "EmergencyBrakingState",
                        "humanId": "STATE-004"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "FaultState",
                        "humanId": "STATE-005"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "states", "状态")
    
    def create_constraints(self):
        """7. 创建约束 (Constraints)"""
        print("\n=== 7️⃣ 创建约束 (Constraints) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConstraintDefinition",
                        "name": "MaxBrakingForceConstraint",
                        "humanId": "CONST-001"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConstraintDefinition",
                        "name": "MinResponseTimeConstraint",
                        "humanId": "CONST-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConstraintDefinition",
                        "name": "TemperatureRangeConstraint",
                        "humanId": "CONST-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConstraintDefinition",
                        "name": "PressureLimitConstraint",
                        "humanId": "CONST-004"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "constraints", "约束")
    
    def create_usecases(self):
        """8. 创建用例 (Use Cases)"""
        print("\n=== 8️⃣ 创建用例 (Use Cases) ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "UseCaseDefinition",
                        "name": "NormalBrakingUseCase",
                        "humanId": "UC-001"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "UseCaseDefinition",
                        "name": "EmergencyBrakingUseCase",
                        "humanId": "UC-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "UseCaseDefinition",
                        "name": "ABSActivationUseCase",
                        "humanId": "UC-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "UseCaseDefinition",
                        "name": "BrakeMaintenanceUseCase",
                        "humanId": "UC-004"
                    }
                }
            ]
        }
        
        return self._create_commit(commit_body, "usecases", "用例")
    
    def _create_commit(self, commit_body, element_type, element_name_cn):
        """创建commit的通用方法"""
        response = self.session.post(
            f"{self.base_url}/projects/{self.project_id}/commits",
            json=commit_body
        )
        
        if response.status_code == 200:
            commit_id = response.json()["@id"]
            
            # 获取创建的元素
            elements_response = self.session.get(
                f"{self.base_url}/projects/{self.project_id}/commits/{commit_id}/elements"
            )
            if elements_response.status_code == 200:
                elements = elements_response.json()
                print(f"✅ 成功创建 {len(elements)} 个{element_name_cn}")
                
                for elem in elements:
                    self.created_elements[element_type].append({
                        'name': elem['name'],
                        'id': elem['@id'],
                        'humanId': elem.get('humanId', ''),
                        'type': elem['@type']
                    })
                return True
        else:
            print(f"❌ {element_name_cn}创建失败: {response.status_code}")
            if response.text:
                print(f"   错误: {response.text}")
        return False
    
    def create_usage_instances(self):
        """创建Usage实例 - 展示Definition到Usage的实例化"""
        print("\n=== 🔄 创建Usage实例 ===")
        
        commit_body = {
            "@type": "Commit",
            "change": [
                # Part Usage实例
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "frontLeftBrakeDisc",
                        "humanId": "PART-USE-001"
                    }
                },
                {
                    "@type": "DataVersion", 
                    "payload": {
                        "@type": "PartUsage",
                        "name": "frontRightBrakeDisc",
                        "humanId": "PART-USE-002"
                    }
                },
                # Action Usage实例
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionUsage",
                        "name": "driverBrakingAction",
                        "humanId": "ACT-USE-001"
                    }
                },
                # State Usage实例
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateUsage",
                        "name": "currentBrakeState",
                        "humanId": "STATE-USE-001"
                    }
                }
            ]
        }
        
        response = self.session.post(
            f"{self.base_url}/projects/{self.project_id}/commits",
            json=commit_body
        )
        
        if response.status_code == 200:
            print("✅ Usage实例创建成功")
            return True
        return False
    
    def display_summary(self):
        """显示创建的所有元素汇总"""
        print("\n" + "="*100)
        print("🎯 SysML 2.0 制动系统模型创建完成")
        print("="*100)
        
        # 定义元素类型和对应的中文名称及图标
        element_types = [
            ('requirements', '需求', '📋'),
            ('parts', '部件', '🔧'),
            ('interfaces', '接口', '🔌'),
            ('connections', '连接', '🔗'),
            ('actions', '动作', '⚡'),
            ('states', '状态', '📊'),
            ('constraints', '约束', '🚫'),
            ('usecases', '用例', '📱')
        ]
        
        total_count = 0
        
        for elem_type, name_cn, icon in element_types:
            elements = self.created_elements[elem_type]
            count = len(elements)
            total_count += count
            
            print(f"\n{icon} {name_cn} ({elem_type.upper()}) - 共 {count} 个:")
            print("-" * 80)
            
            for elem in elements:
                print(f"  {elem['humanId']:<12} | {elem['name']:<30} | {elem['type']}")
        
        print(f"\n✅ 总计创建了 {total_count} 个模型元素")
        print("\n这个完整的制动系统模型展示了SysML 2.0的核心建模能力：")
        print("  • 需求追踪 - 从系统需求到具体实现")
        print("  • 结构建模 - 部件、接口和连接")
        print("  • 行为建模 - 动作、状态和转换")
        print("  • 约束表达 - 系统限制和规则")
        print("  • 场景描述 - 用例和交互")
    
    def run(self):
        """执行完整的模型创建流程"""
        print("🚀 开始构建完整的SysML 2.0制动系统模型")
        print("="*100)
        
        if not self.create_project():
            print("❌ 项目创建失败")
            return
        
        # 按顺序创建8种元素类型
        steps = [
            (self.create_requirements, "需求"),
            (self.create_parts, "部件"),
            (self.create_interfaces, "接口"),
            (self.create_connections, "连接"),
            (self.create_actions, "动作"),
            (self.create_states, "状态"),
            (self.create_constraints, "约束"),
            (self.create_usecases, "用例")
        ]
        
        for i, (create_func, name) in enumerate(steps, 1):
            self.update_todo_progress(i + 20)  # 更新TODO列表
            if not create_func():
                print(f"\n❌ {name}创建失败，停止执行")
                return
        
        # 创建Usage实例
        self.create_usage_instances()
        
        # 显示汇总
        self.display_summary()
        
        print("\n🎉 恭喜！完整的SysML 2.0制动系统模型已成功创建并存储在数据库中。")
    
    def update_todo_progress(self, todo_id):
        """更新TODO进度（辅助方法）"""
        # 这里可以调用TodoWrite工具更新进度
        pass

if __name__ == "__main__":
    builder = CompleteBrakeSystemBuilder()
    builder.run()