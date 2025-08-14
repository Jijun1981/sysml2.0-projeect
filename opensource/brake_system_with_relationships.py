#!/usr/bin/env python3
"""
构建带有丰富关系的SysML 2.0制动系统模型
包含以下关系类型：
1. 满足关系 (Satisfy) - 部件满足需求
2. 分解关系 (Decomposition) - 系统分解为子系统
3. 连接关系 (Connection) - 部件之间的物理/逻辑连接
4. 分配关系 (Allocation) - 功能分配到结构
5. 继承关系 (Specialization) - 特殊化/泛化
6. 依赖关系 (Dependency) - 元素间的依赖
7. 接口实现 (InterfaceRealization) - 部件实现接口
8. 状态转换 (Transition) - 状态之间的转换
"""

import requests
import json
from datetime import datetime

class BrakeSystemWithRelationships:
    def __init__(self):
        self.base_url = "http://localhost:9000"
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.project_id = None
        self.element_ids = {}  # 存储创建的元素ID，用于建立关系
        
    def create_project(self):
        """创建项目"""
        project_data = {
            "@type": "Project",
            "name": "BrakeSystemWithRelationships",
            "description": "带有丰富关系的SysML 2.0制动系统模型"
        }
        
        response = self.session.post(f"{self.base_url}/projects", json=project_data)
        if response.status_code == 200:
            project = response.json()
            self.project_id = project["@id"]
            print(f"✅ 项目创建成功: {project['name']}")
            return True
        return False
    
    def create_base_elements(self):
        """创建基础元素"""
        print("\n=== 🏗️ 创建基础元素 ===")
        
        # 1. 创建包结构
        print("\n1️⃣ 创建包结构...")
        package_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "Package",
                        "name": "BrakeSystemModel",
                        "@id": "pkg-main"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "Package",
                        "name": "Requirements",
                        "@id": "pkg-req"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "Package",
                        "name": "Structure",
                        "@id": "pkg-struct"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "Package",
                        "name": "Behavior",
                        "@id": "pkg-behavior"
                    }
                }
            ]
        }
        
        if not self._execute_commit(package_commit, "包"):
            return False
        
        # 2. 创建需求层次
        print("\n2️⃣ 创建需求层次...")
        req_commit = {
            "@type": "Commit",
            "change": [
                # 顶层需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "SystemSafetyRequirement",
                        "@id": "req-top-safety",
                        "text": "制动系统必须确保车辆和乘客安全"
                    }
                },
                # 功能需求（继承自顶层需求）
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "BrakingPerformanceRequirement",
                        "@id": "req-performance",
                        "text": "制动性能必须满足法规要求",
                        "general": [{"@id": "req-top-safety"}]  # 继承关系
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "RequirementDefinition",
                        "name": "ResponseTimeRequirement",
                        "@id": "req-response",
                        "text": "响应时间<150ms",
                        "general": [{"@id": "req-performance"}]  # 继承关系
                    }
                },
                # 约束需求
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConstraintDefinition",
                        "name": "BrakingForceConstraint",
                        "@id": "const-force",
                        "text": "制动力 >= 0.8 * 车重"
                    }
                }
            ]
        }
        
        if not self._execute_commit(req_commit, "需求"):
            return False
            
        # 3. 创建部件层次（带继承）
        print("\n3️⃣ 创建部件层次...")
        part_commit = {
            "@type": "Commit",
            "change": [
                # 抽象基类
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeComponent",
                        "@id": "part-abstract-component",
                        "isAbstract": True
                    }
                },
                # 制动系统（顶层）
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "VehicleBrakeSystem",
                        "@id": "part-brake-system",
                        "general": [{"@id": "part-abstract-component"}]
                    }
                },
                # 液压子系统
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "HydraulicSubsystem",
                        "@id": "part-hydraulic",
                        "general": [{"@id": "part-abstract-component"}]
                    }
                },
                # 具体部件
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakePedal",
                        "@id": "part-pedal"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "MasterCylinder",
                        "@id": "part-master"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "BrakeCaliper",
                        "@id": "part-caliper",
                        "general": [{"@id": "part-abstract-component"}]
                    }
                },
                # 特殊化的卡钳
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartDefinition",
                        "name": "HighPerformanceCaliper",
                        "@id": "part-hp-caliper",
                        "general": [{"@id": "part-caliper"}]  # 继承自BrakeCaliper
                    }
                }
            ]
        }
        
        if not self._execute_commit(part_commit, "部件"):
            return False
            
        # 4. 创建接口
        print("\n4️⃣ 创建接口...")
        interface_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceDefinition",
                        "name": "IMechanicalForce",
                        "@id": "intf-mechanical"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceDefinition",
                        "name": "IHydraulicPressure",
                        "@id": "intf-hydraulic"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceDefinition",
                        "name": "IElectricalSignal",
                        "@id": "intf-electrical"
                    }
                }
            ]
        }
        
        if not self._execute_commit(interface_commit, "接口"):
            return False
            
        # 5. 创建行为（动作和状态）
        print("\n5️⃣ 创建行为元素...")
        behavior_commit = {
            "@type": "Commit",
            "change": [
                # 动作
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionDefinition",
                        "name": "ApplyBrake",
                        "@id": "act-apply"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ActionDefinition",
                        "name": "ModulatePressure",
                        "@id": "act-modulate"
                    }
                },
                # 状态
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "Released",
                        "@id": "state-released"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "Applied",
                        "@id": "state-applied"
                    }
                },
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "StateDefinition",
                        "name": "ABSActive",
                        "@id": "state-abs"
                    }
                }
            ]
        }
        
        if not self._execute_commit(behavior_commit, "行为"):
            return False
            
        return True
    
    def create_relationships(self):
        """创建各种关系"""
        print("\n=== 🔗 创建关系 ===")
        
        # 1. 部件组成关系（Part Usage）
        print("\n1️⃣ 创建部件组成关系...")
        composition_commit = {
            "@type": "Commit",
            "change": [
                # 制动系统包含液压子系统
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "hydraulics",
                        "@id": "usage-hydraulics",
                        "definition": {"@id": "part-hydraulic"},
                        "owningDefinition": {"@id": "part-brake-system"}
                    }
                },
                # 液压子系统包含主缸
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "masterCyl",
                        "@id": "usage-master",
                        "definition": {"@id": "part-master"},
                        "owningDefinition": {"@id": "part-hydraulic"}
                    }
                },
                # 制动系统包含踏板
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "pedal",
                        "@id": "usage-pedal",
                        "definition": {"@id": "part-pedal"},
                        "owningDefinition": {"@id": "part-brake-system"}
                    }
                }
            ]
        }
        
        if not self._execute_commit(composition_commit, "组成关系"):
            return False
        
        # 2. 连接关系
        print("\n2️⃣ 创建连接关系...")
        connection_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "ConnectionDefinition",
                        "name": "PedalToMasterConnection",
                        "@id": "conn-pedal-master",
                        "ends": [
                            {"@id": "usage-pedal"},
                            {"@id": "usage-master"}
                        ]
                    }
                }
            ]
        }
        
        if not self._execute_commit(connection_commit, "连接"):
            return False
        
        # 3. 满足关系（部件满足需求）
        print("\n3️⃣ 创建满足关系...")
        satisfy_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "SatisfyRequirementUsage",
                        "name": "SystemSatisfiesPerformance",
                        "@id": "satisfy-1",
                        "satisfiedRequirement": {"@id": "req-performance"},
                        "satisfyingFeature": {"@id": "part-brake-system"}
                    }
                }
            ]
        }
        
        if not self._execute_commit(satisfy_commit, "满足关系"):
            return False
        
        # 4. 分配关系（功能分配到结构）
        print("\n4️⃣ 创建分配关系...")
        allocation_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "AllocationDefinition",
                        "name": "ApplyBrakeAllocation",
                        "@id": "alloc-1",
                        "source": [{"@id": "act-apply"}],
                        "target": [{"@id": "part-pedal"}]
                    }
                }
            ]
        }
        
        if not self._execute_commit(allocation_commit, "分配关系"):
            return False
        
        # 5. 接口实现关系
        print("\n5️⃣ 创建接口实现关系...")
        interface_real_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "InterfaceUsage",
                        "name": "pedalMechanicalInterface",
                        "@id": "intf-usage-1",
                        "definition": {"@id": "intf-mechanical"},
                        "owningDefinition": {"@id": "part-pedal"}
                    }
                }
            ]
        }
        
        if not self._execute_commit(interface_real_commit, "接口实现"):
            return False
        
        # 6. 状态转换
        print("\n6️⃣ 创建状态转换...")
        transition_commit = {
            "@type": "Commit",
            "change": [
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "TransitionUsage",
                        "name": "ReleasedToApplied",
                        "@id": "trans-1",
                        "source": {"@id": "state-released"},
                        "target": {"@id": "state-applied"},
                        "trigger": {"@id": "act-apply"}
                    }
                }
            ]
        }
        
        if not self._execute_commit(transition_commit, "状态转换"):
            return False
            
        return True
    
    def create_instances(self):
        """创建实例（展示Definition到Usage的映射）"""
        print("\n=== 🎯 创建实例 ===")
        
        instance_commit = {
            "@type": "Commit",
            "change": [
                # 前左制动卡钳实例
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "frontLeftCaliper",
                        "@id": "instance-fl-caliper",
                        "definition": {"@id": "part-hp-caliper"},  # 使用高性能卡钳
                        "individualDefinition": {"@id": "part-hp-caliper"}
                    }
                },
                # 前右制动卡钳实例
                {
                    "@type": "DataVersion",
                    "payload": {
                        "@type": "PartUsage",
                        "name": "frontRightCaliper",
                        "@id": "instance-fr-caliper",
                        "definition": {"@id": "part-hp-caliper"}
                    }
                }
            ]
        }
        
        return self._execute_commit(instance_commit, "实例")
    
    def _execute_commit(self, commit_body, element_type_name):
        """执行commit并返回结果"""
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
                
                # 存储元素ID
                for elem in elements:
                    if "@id" in elem:
                        self.element_ids[elem["@id"]] = elem
                        
                return True
        
        print(f"❌ {element_type_name}创建失败: {response.status_code}")
        if response.text:
            print(f"   错误: {response.text}")
        return False
    
    def display_relationship_diagram(self):
        """显示关系图"""
        print("\n" + "="*100)
        print("🎨 制动系统模型关系图")
        print("="*100)
        
        print("""
        📋 SystemSafetyRequirement
                    ↑ [继承]
        📋 BrakingPerformanceRequirement  <--[满足]-- 🔧 VehicleBrakeSystem
                    ↑ [继承]                              ├── 🔧 hydraulics: HydraulicSubsystem
        📋 ResponseTimeRequirement                        │       └── 🔧 masterCyl: MasterCylinder
                                                         └── 🔧 pedal: BrakePedal --[连接]--> MasterCylinder
                                                                    ↓ [分配]
                                                              ⚡ ApplyBrake
        
        🏭 BrakeComponent (抽象)
             ↑ [继承]
        ┌────┴────┬──────────────┐
        │         │              │
    🔧 VehicleBrakeSystem  🔧 HydraulicSubsystem  🔧 BrakeCaliper
                                                        ↑ [特殊化]
                                                   🔧 HighPerformanceCaliper
                                                        ↓ [实例化]
                                            ┌───────────┴───────────┐
                                      🎯 frontLeftCaliper   🎯 frontRightCaliper
        
        状态机:
        📊 Released --[ApplyBrake触发]--> 📊 Applied --[检测到打滑]--> 📊 ABSActive
        
        接口实现:
        🔧 BrakePedal --[实现]--> 🔌 IMechanicalForce
        """)
        
        print("\n关系类型说明:")
        print("  • 继承 (Generalization): 子类型继承父类型的特性")
        print("  • 组成 (Composition): 整体-部分关系")
        print("  • 满足 (Satisfy): 设计元素满足需求")
        print("  • 分配 (Allocation): 功能分配到结构")
        print("  • 连接 (Connection): 部件之间的物理/逻辑连接")
        print("  • 实例化 (Instantiation): Definition到Usage的映射")
        print("  • 接口实现 (Interface Realization): 部件实现接口契约")
        print("  • 状态转换 (Transition): 状态之间的转换关系")
    
    def run(self):
        """执行完整流程"""
        print("🚀 开始构建带有丰富关系的SysML 2.0制动系统模型")
        print("="*100)
        
        if not self.create_project():
            print("❌ 项目创建失败")
            return
            
        if not self.create_base_elements():
            print("❌ 基础元素创建失败")
            return
            
        if not self.create_relationships():
            print("❌ 关系创建失败")
            return
            
        if not self.create_instances():
            print("❌ 实例创建失败")
            return
            
        self.display_relationship_diagram()
        
        print("\n✅ 成功创建了一个包含丰富关系的SysML 2.0制动系统模型！")
        print("   该模型展示了：")
        print("   • 需求层次和追踪")
        print("   • 部件继承和组成")
        print("   • 接口定义和实现")
        print("   • 行为分配和状态机")
        print("   • 实例化和特殊化")

if __name__ == "__main__":
    builder = BrakeSystemWithRelationships()
    builder.run()