#!/usr/bin/env python3
"""
按照Phase3-追踪矩阵.yaml从W2到W4实现制动系统模型
包含：
- W2: Requirements (需求定义和使用)
- W3: Parts + Ports (部件和端口)
- W4: Connectors + Satisfy (连接和满足关系)
"""

import requests
import json
from datetime import datetime

class BrakeSystemW2ToW4:
    def __init__(self):
        self.base_url = "http://localhost:9000"
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.project_id = None
        self.elements = {
            'requirements': {},
            'parts': {},
            'ports': {},
            'connectors': {},
            'satisfies': {}
        }
    
    def create_project(self):
        """创建制动系统项目"""
        project_data = {
            "@type": "Project",
            "name": "BrakeSystemW2W4Demo",
            "description": "制动系统W2-W4完整演示"
        }
        
        response = self.session.post(f"{self.base_url}/projects", json=project_data)
        if response.status_code == 200:
            project = response.json()
            self.project_id = project["@id"]
            print(f"✅ 项目创建成功: {project['name']}")
            return True
        return False
    
    # ========== W2: Requirements ==========
    def create_w2_requirements(self):
        """W2: 创建需求定义和使用"""
        print("\n=== W2: 创建需求 (Requirements) ===")
        
        # W2 Story 2.1.1: RequirementDefinition实例
        print("\n📋 W2-E2.1-S1.1: 创建RequirementDefinition...")
        req_def_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "SafeBrakingReq",
                        "@id": "req-def-001",
                        "text": "制动系统应在100km/h速度下40米内完全停止"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "ResponseTimeReq",
                        "@id": "req-def-002",
                        "text": "制动响应时间不超过150ms"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "ReliabilityReq",
                        "@id": "req-def-003",
                        "text": "制动系统MTBF大于100000小时"
                    }
                }
            ]
        }
        
        if self._execute_commit(req_def_commit, "RequirementDefinition"):
            self.elements['requirements']['SafeBrakingReq'] = 'req-def-001'
            self.elements['requirements']['ResponseTimeReq'] = 'req-def-002'
            self.elements['requirements']['ReliabilityReq'] = 'req-def-003'
        
        # W2 Story 2.1.2: RequirementUsage实例
        print("\n📋 W2-E2.1-S1.2: 创建RequirementUsage...")
        req_usage_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementUsage",
                        "name": "vehicleBrakingReq",
                        "@id": "req-use-001",
                        "definition": {"@id": "req-def-001"},
                        "text": "具体车辆制动距离要求"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementUsage",
                        "name": "absResponseReq",
                        "@id": "req-use-002",
                        "definition": {"@id": "req-def-002"},
                        "text": "ABS系统响应时间要求"
                    }
                }
            ]
        }
        
        if self._execute_commit(req_usage_commit, "RequirementUsage"):
            self.elements['requirements']['vehicleBrakingReq'] = 'req-use-001'
            self.elements['requirements']['absResponseReq'] = 'req-use-002'
            
        return True
    
    # ========== W3: Parts + Ports ==========
    def create_w3_parts_ports(self):
        """W3: 创建部件和端口"""
        print("\n=== W3: 创建部件和端口 (Parts + Ports) ===")
        
        # W3 Story 3.1.1: PartDefinition实例
        print("\n🔧 W3-E3.1-S1.1: 创建PartDefinition...")
        part_def_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeSystem",
                        "@id": "part-def-001"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakePedal",
                        "@id": "part-def-002"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "MasterCylinder",
                        "@id": "part-def-003"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeCaliper",
                        "@id": "part-def-004"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "ABSController",
                        "@id": "part-def-005"
                    }
                }
            ]
        }
        
        if self._execute_commit(part_def_commit, "PartDefinition"):
            self.elements['parts']['BrakeSystem'] = 'part-def-001'
            self.elements['parts']['BrakePedal'] = 'part-def-002'
            self.elements['parts']['MasterCylinder'] = 'part-def-003'
            self.elements['parts']['BrakeCaliper'] = 'part-def-004'
            self.elements['parts']['ABSController'] = 'part-def-005'
        
        # W3 Story 3.1.2: PartUsage实例
        print("\n🔧 W3-E3.1-S1.2: 创建PartUsage...")
        part_usage_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "vehicleBrakeSystem",
                        "@id": "part-use-001",
                        "definition": {"@id": "part-def-001"}
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "pedal",
                        "@id": "part-use-002",
                        "definition": {"@id": "part-def-002"},
                        "owningUsage": {"@id": "part-use-001"}
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "masterCyl",
                        "@id": "part-use-003",
                        "definition": {"@id": "part-def-003"},
                        "owningUsage": {"@id": "part-use-001"}
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "frontLeftCaliper",
                        "@id": "part-use-004",
                        "definition": {"@id": "part-def-004"},
                        "owningUsage": {"@id": "part-use-001"}
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "absModule",
                        "@id": "part-use-005",
                        "definition": {"@id": "part-def-005"},
                        "owningUsage": {"@id": "part-use-001"}
                    }
                }
            ]
        }
        
        if self._execute_commit(part_usage_commit, "PartUsage"):
            self.elements['parts']['vehicleBrakeSystem'] = 'part-use-001'
            self.elements['parts']['pedal'] = 'part-use-002'
            self.elements['parts']['masterCyl'] = 'part-use-003'
            self.elements['parts']['frontLeftCaliper'] = 'part-use-004'
            self.elements['parts']['absModule'] = 'part-use-005'
        
        # W3 Story 3.2.1: PortUsage实例
        print("\n🔌 W3-E3.2-S1.1: 创建PortUsage...")
        port_usage_commit = {
            "@type": "Commit",
            "change": [
                # 踏板的输出端口
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PortUsage",
                        "name": "pedalForceOut",
                        "@id": "port-001",
                        "owningUsage": {"@id": "part-use-002"}
                    }
                },
                # 主缸的输入和输出端口
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PortUsage",
                        "name": "forceIn",
                        "@id": "port-002",
                        "owningUsage": {"@id": "part-use-003"}
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PortUsage",
                        "name": "pressureOut",
                        "@id": "port-003",
                        "owningUsage": {"@id": "part-use-003"}
                    }
                },
                # 卡钳的输入端口
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PortUsage",
                        "name": "pressureIn",
                        "@id": "port-004",
                        "owningUsage": {"@id": "part-use-004"}
                    }
                },
                # ABS的信号端口
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PortUsage",
                        "name": "controlSignal",
                        "@id": "port-005",
                        "owningUsage": {"@id": "part-use-005"}
                    }
                }
            ]
        }
        
        if self._execute_commit(port_usage_commit, "PortUsage"):
            self.elements['ports']['pedalForceOut'] = 'port-001'
            self.elements['ports']['forceIn'] = 'port-002'
            self.elements['ports']['pressureOut'] = 'port-003'
            self.elements['ports']['pressureIn'] = 'port-004'
            self.elements['ports']['controlSignal'] = 'port-005'
            
        return True
    
    # ========== W4: Connectors + Satisfy ==========
    def create_w4_connections_satisfy(self):
        """W4: 创建连接和满足关系"""
        print("\n=== W4: 创建连接和满足关系 (Connectors + Satisfy) ===")
        
        # W4 Story 4.1.1: ConnectorUsage实例
        print("\n🔗 W4-E4.1-S1.1: 创建ConnectorUsage...")
        connector_commit = {
            "@type": "Commit",
            "change": [
                # 踏板到主缸的连接
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectorUsage",
                        "name": "pedalToMasterConnection",
                        "@id": "conn-001",
                        "ends": [
                            {"@id": "port-001"},  # pedalForceOut
                            {"@id": "port-002"}   # forceIn
                        ]
                    }
                },
                # 主缸到卡钳的连接
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectorUsage",
                        "name": "masterToCaliperConnection",
                        "@id": "conn-002",
                        "ends": [
                            {"@id": "port-003"},  # pressureOut
                            {"@id": "port-004"}   # pressureIn
                        ]
                    }
                }
            ]
        }
        
        if self._execute_commit(connector_commit, "ConnectorUsage"):
            self.elements['connectors']['pedalToMasterConnection'] = 'conn-001'
            self.elements['connectors']['masterToCaliperConnection'] = 'conn-002'
        
        # W4 Story 4.2.1: SatisfyRequirementUsage实例
        print("\n✅ W4-E4.2-S1.1: 创建SatisfyRequirementUsage...")
        satisfy_commit = {
            "@type": "Commit",
            "change": [
                # 制动系统满足制动距离需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "SatisfyRequirementUsage",
                        "name": "brakeSystemSatisfiesDistance",
                        "@id": "satisfy-001",
                        "satisfiedRequirement": {"@id": "req-use-001"},  # vehicleBrakingReq
                        "satisfyingFeature": {"@id": "part-use-001"}     # vehicleBrakeSystem
                    }
                },
                # ABS模块满足响应时间需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "SatisfyRequirementUsage",
                        "name": "absSatisfiesResponseTime",
                        "@id": "satisfy-002",
                        "satisfiedRequirement": {"@id": "req-use-002"},  # absResponseReq
                        "satisfyingFeature": {"@id": "part-use-005"}     # absModule
                    }
                }
            ]
        }
        
        if self._execute_commit(satisfy_commit, "SatisfyRequirementUsage"):
            self.elements['satisfies']['brakeSystemSatisfiesDistance'] = 'satisfy-001'
            self.elements['satisfies']['absSatisfiesResponseTime'] = 'satisfy-002'
            
        return True
    
    def create_constraints(self):
        """创建约束关系"""
        print("\n=== 创建约束 (Constraints) ===")
        
        constraint_commit = {
            "@type": "Commit",
            "change": [
                # 制动力约束
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConstraintDefinition",
                        "name": "BrakingForceConstraint",
                        "@id": "const-001",
                        "text": "制动力 >= 0.8 * 车重"
                    }
                },
                # 响应时间约束
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConstraintUsage",
                        "name": "responseTimeLimit",
                        "@id": "const-use-001",
                        "definition": {"@id": "const-001"},
                        "constrainedElement": {"@id": "part-use-005"}  # ABS模块
                    }
                }
            ]
        }
        
        return self._execute_commit(constraint_commit, "Constraint")
    
    def _execute_commit(self, commit_body, element_type_name):
        """执行commit"""
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
                print(f"✅ 成功创建 {len(elements)} 个{element_type_name}")
                return True
        
        print(f"❌ {element_type_name}创建失败: {response.status_code}")
        if response.text:
            print(f"   错误: {response.text}")
        return False
    
    def display_model_summary(self):
        """显示模型摘要"""
        print("\n" + "="*80)
        print("🎯 制动系统模型创建完成 (W2-W4)")
        print("="*80)
        
        print("\n📋 W2 - 需求层次:")
        print("  需求定义:")
        print("    • SafeBrakingReq - 制动距离要求")
        print("    • ResponseTimeReq - 响应时间要求")
        print("    • ReliabilityReq - 可靠性要求")
        print("  需求使用:")
        print("    • vehicleBrakingReq → 具体车辆制动要求")
        print("    • absResponseReq → ABS响应时间要求")
        
        print("\n🔧 W3 - 部件结构:")
        print("  vehicleBrakeSystem (制动系统)")
        print("    ├── pedal (踏板) [端口: pedalForceOut]")
        print("    ├── masterCyl (主缸) [端口: forceIn, pressureOut]")
        print("    ├── frontLeftCaliper (卡钳) [端口: pressureIn]")
        print("    └── absModule (ABS模块) [端口: controlSignal]")
        
        print("\n🔗 W4 - 连接关系:")
        print("  物理连接:")
        print("    • pedalForceOut → forceIn (踏板到主缸)")
        print("    • pressureOut → pressureIn (主缸到卡钳)")
        print("  满足关系:")
        print("    • vehicleBrakeSystem 满足 vehicleBrakingReq")
        print("    • absModule 满足 absResponseReq")
        
        print("\n这个模型展示了SysML 2.0的核心建模能力：")
        print("  • 需求定义和分解 (Definition → Usage)")
        print("  • 部件层次和组成")
        print("  • 端口和连接建模")
        print("  • 需求追溯关系")
        print("  • 约束定义和应用")
    
    def run(self):
        """执行W2-W4流程"""
        print("🚀 开始创建制动系统模型 (W2-W4)")
        print("="*80)
        
        if not self.create_project():
            print("❌ 项目创建失败")
            return
        
        # 按周执行
        if not self.create_w2_requirements():
            print("❌ W2需求创建失败")
            return
            
        if not self.create_w3_parts_ports():
            print("❌ W3部件端口创建失败")
            return
            
        if not self.create_w4_connections_satisfy():
            print("❌ W4连接满足关系创建失败")
            return
            
        if not self.create_constraints():
            print("❌ 约束创建失败")
            return
            
        self.display_model_summary()
        
        print("\n✅ 恭喜！W2-W4制动系统模型创建完成！")

if __name__ == "__main__":
    builder = BrakeSystemW2ToW4()
    builder.run()