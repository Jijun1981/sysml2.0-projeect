#!/usr/bin/env python3
"""
创建完整的制动系统模型，包含所有元素关系
"""
import requests
import json
import uuid
from datetime import datetime

# API基础配置
BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}  # 使用application/json

def create_project():
    """创建新项目"""
    project_body = {
        "@type": "Project",
        "name": f"CompleteBrakeSystem_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
        "description": "Complete brake system model with all relationships"
    }
    
    response = requests.post(f"{BASE_URL}/projects", json=project_body, headers=headers)
    if response.status_code != 200:
        raise Exception(f"Failed to create project: {response.status_code}")
    
    return response.json()["@id"]

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
        return None
    
    element_id = commit_body["change"][0]["payload"]["@id"]
    print(f"✅ Created {element_type}: {element_data.get('name', 'unnamed')} ({element_id})")
    return element_id

def main():
    try:
        # 创建项目
        project_id = create_project()
        print(f"🚀 Created project: {project_id}\n")
        
        # ========= 1. 创建需求 (Requirements) =========
        print("=== 1. Creating Requirements ===")
        requirements = {}
        
        requirements['func_req'] = create_element(project_id, "RequirementDefinition", {
            "name": "FunctionalRequirement",
            "text": ["The brake system shall stop the vehicle within specified distance"],
            "reqId": "FUNC-001"
        })
        
        requirements['perf_req'] = create_element(project_id, "RequirementDefinition", {
            "name": "PerformanceRequirement",
            "text": ["Braking distance shall not exceed 40 meters from 100 km/h"],
            "reqId": "PERF-001"
        })
        
        requirements['safety_req'] = create_element(project_id, "RequirementDefinition", {
            "name": "SafetyRequirement",
            "text": ["System shall maintain redundancy for critical components"],
            "reqId": "SAFE-001"
        })
        
        requirements['response_req'] = create_element(project_id, "RequirementDefinition", {
            "name": "ResponseTimeRequirement",
            "text": ["System shall engage within 150ms"],
            "reqId": "RESP-001"
        })
        
        # ========= 2. 创建部件定义 (Part Definitions) =========
        print("\n=== 2. Creating Part Definitions ===")
        parts = {}
        
        parts['brake_system'] = create_element(project_id, "PartDefinition", {
            "name": "BrakeSystemAssembly",
            "shortName": "BSA"
        })
        
        parts['pedal'] = create_element(project_id, "PartDefinition", {
            "name": "BrakePedal",
            "shortName": "BP"
        })
        
        parts['master_cylinder'] = create_element(project_id, "PartDefinition", {
            "name": "MasterCylinder",
            "shortName": "MC"
        })
        
        parts['brake_line'] = create_element(project_id, "PartDefinition", {
            "name": "BrakeLine",
            "shortName": "BL"
        })
        
        parts['caliper'] = create_element(project_id, "PartDefinition", {
            "name": "BrakeCaliper",
            "shortName": "BC"
        })
        
        parts['brake_pad'] = create_element(project_id, "PartDefinition", {
            "name": "BrakePad",
            "shortName": "BPD"
        })
        
        parts['brake_disc'] = create_element(project_id, "PartDefinition", {
            "name": "BrakeDisc",
            "shortName": "BD"
        })
        
        # ========= 3. 创建端口定义 (Port Definitions) =========
        print("\n=== 3. Creating Port Definitions ===")
        ports = {}
        
        ports['pedal_input'] = create_element(project_id, "PortDefinition", {
            "name": "PedalInputPort"
        })
        
        ports['hydraulic_output'] = create_element(project_id, "PortDefinition", {
            "name": "HydraulicOutputPort"
        })
        
        ports['hydraulic_input'] = create_element(project_id, "PortDefinition", {
            "name": "HydraulicInputPort"
        })
        
        ports['mechanical_output'] = create_element(project_id, "PortDefinition", {
            "name": "MechanicalOutputPort"
        })
        
        # ========= 4. 创建接口定义 (Interface Definitions) =========
        print("\n=== 4. Creating Interface Definitions ===")
        interfaces = {}
        
        interfaces['hydraulic'] = create_element(project_id, "InterfaceDefinition", {
            "name": "HydraulicInterface"
        })
        
        interfaces['mechanical'] = create_element(project_id, "InterfaceDefinition", {
            "name": "MechanicalInterface"
        })
        
        interfaces['electrical'] = create_element(project_id, "InterfaceDefinition", {
            "name": "ElectricalInterface"
        })
        
        # ========= 5. 创建部件使用 (Part Usages) =========
        print("\n=== 5. Creating Part Usages ===")
        part_usages = {}
        
        part_usages['pedal_usage'] = create_element(project_id, "PartUsage", {
            "name": "pedalInstance"
        })
        
        part_usages['mc_usage'] = create_element(project_id, "PartUsage", {
            "name": "masterCylinderInstance"
        })
        
        part_usages['caliper_fl'] = create_element(project_id, "PartUsage", {
            "name": "caliperFrontLeft"
        })
        
        part_usages['caliper_fr'] = create_element(project_id, "PartUsage", {
            "name": "caliperFrontRight"
        })
        
        # ========= 6. 创建端口使用 (Port Usages) =========
        print("\n=== 6. Creating Port Usages ===")
        port_usages = {}
        
        port_usages['pedal_port'] = create_element(project_id, "PortUsage", {
            "name": "pedalPort"
        })
        
        port_usages['mc_in_port'] = create_element(project_id, "PortUsage", {
            "name": "mcInputPort"
        })
        
        port_usages['mc_out_port'] = create_element(project_id, "PortUsage", {
            "name": "mcOutputPort"
        })
        
        # ========= 7. 创建连接 (Connections) =========
        print("\n=== 7. Creating Connections ===")
        connections = {}
        
        connections['pedal_to_mc'] = create_element(project_id, "ConnectionUsage", {
            "name": "pedalToMasterCylinderConnection"
        })
        
        connections['mc_to_caliper'] = create_element(project_id, "ConnectionUsage", {
            "name": "masterCylinderToCaliperConnection"
        })
        
        # ========= 8. 创建绑定连接器 (Binding Connectors) =========
        print("\n=== 8. Creating Binding Connectors ===")
        bindings = {}
        
        bindings['pedal_mc_binding'] = create_element(project_id, "BindingConnector", {
            "name": "pedalMCBinding"
        })
        
        # ========= 9. 创建满足关系 (Satisfy Requirements) =========
        print("\n=== 9. Creating Satisfy Relationships ===")
        satisfies = {}
        
        satisfies['system_func'] = create_element(project_id, "SatisfyRequirementUsage", {
            "name": "systemSatisfiesFunctional"
        })
        
        satisfies['pedal_response'] = create_element(project_id, "SatisfyRequirementUsage", {
            "name": "pedalSatisfiesResponseTime"
        })
        
        satisfies['caliper_perf'] = create_element(project_id, "SatisfyRequirementUsage", {
            "name": "caliperSatisfiesPerformance"
        })
        
        # ========= 10. 创建分配关系 (Allocations) =========
        print("\n=== 10. Creating Allocations ===")
        allocations = {}
        
        allocations['func_to_system'] = create_element(project_id, "AllocationUsage", {
            "name": "functionalToSystemAllocation"
        })
        
        allocations['perf_to_caliper'] = create_element(project_id, "AllocationUsage", {
            "name": "performanceToCaliperAllocation"
        })
        
        # ========= 11. 创建约束 (Constraints) =========
        print("\n=== 11. Creating Constraints ===")
        constraints = {}
        
        constraints['pressure_constraint'] = create_element(project_id, "ConstraintDefinition", {
            "name": "HydraulicPressureConstraint"
        })
        
        constraints['force_constraint'] = create_element(project_id, "ConstraintDefinition", {
            "name": "BrakingForceConstraint"
        })
        
        # ========= 12. 创建动作 (Actions) =========
        print("\n=== 12. Creating Actions ===")
        actions = {}
        
        actions['apply_brake'] = create_element(project_id, "ActionDefinition", {
            "name": "ApplyBrakeAction"
        })
        
        actions['release_brake'] = create_element(project_id, "ActionDefinition", {
            "name": "ReleaseBrakeAction"
        })
        
        # ========= 13. 创建状态 (States) =========
        print("\n=== 13. Creating States ===")
        states = {}
        
        states['idle'] = create_element(project_id, "StateDefinition", {
            "name": "IdleState"
        })
        
        states['engaged'] = create_element(project_id, "StateDefinition", {
            "name": "EngagedState"
        })
        
        states['emergency'] = create_element(project_id, "StateDefinition", {
            "name": "EmergencyBrakingState"
        })
        
        # ========= 14. 创建用例 (Use Cases) =========
        print("\n=== 14. Creating Use Cases ===")
        use_cases = {}
        
        use_cases['normal_braking'] = create_element(project_id, "UseCaseDefinition", {
            "name": "NormalBrakingUseCase"
        })
        
        use_cases['emergency_braking'] = create_element(project_id, "UseCaseDefinition", {
            "name": "EmergencyBrakingUseCase"
        })
        
        # ========= 15. 创建验证案例 (Verification Cases) =========
        print("\n=== 15. Creating Verification Cases ===")
        verifications = {}
        
        verifications['brake_test'] = create_element(project_id, "VerificationCaseDefinition", {
            "name": "BrakePerformanceTest"
        })
        
        verifications['safety_test'] = create_element(project_id, "VerificationCaseDefinition", {
            "name": "SafetyRedundancyTest"
        })
        
        # ========= 16. 创建分析案例 (Analysis Cases) =========
        print("\n=== 16. Creating Analysis Cases ===")
        analyses = {}
        
        analyses['stress_analysis'] = create_element(project_id, "AnalysisCaseDefinition", {
            "name": "BrakeStressAnalysis"
        })
        
        analyses['thermal_analysis'] = create_element(project_id, "AnalysisCaseDefinition", {
            "name": "ThermalAnalysis"
        })
        
        # ========= 17. 创建视图 (Views) =========
        print("\n=== 17. Creating Views ===")
        views = {}
        
        views['system_view'] = create_element(project_id, "ViewDefinition", {
            "name": "SystemArchitectureView"
        })
        
        views['req_view'] = create_element(project_id, "ViewDefinition", {
            "name": "RequirementsTraceabilityView"
        })
        
        # ========= 18. 创建关系 (Relationships) =========
        print("\n=== 18. Creating Relationships ===")
        relationships = {}
        
        # 特化关系
        relationships['spec1'] = create_element(project_id, "Specialization", {
            "name": "caliperSpecialization"
        })
        
        # 子集关系
        relationships['subset1'] = create_element(project_id, "Subsetting", {
            "name": "padSubsetting"
        })
        
        # 重定义关系
        relationships['redef1'] = create_element(project_id, "Redefinition", {
            "name": "portRedefinition"
        })
        
        # 依赖关系
        relationships['dep1'] = create_element(project_id, "Dependency", {
            "name": "systemDependency"
        })
        
        # ========= 统计结果 =========
        print("\n" + "="*50)
        print("=== COMPLETE BRAKE SYSTEM MODEL CREATED ===")
        print("="*50)
        print(f"Project ID: {project_id}")
        
        # 统计各类元素数量
        element_counts = {
            "Requirements": len(requirements),
            "Part Definitions": len(parts),
            "Port Definitions": len(ports),
            "Interface Definitions": len(interfaces),
            "Part Usages": len(part_usages),
            "Port Usages": len(port_usages),
            "Connections": len(connections),
            "Binding Connectors": len(bindings),
            "Satisfy Relationships": len(satisfies),
            "Allocations": len(allocations),
            "Constraints": len(constraints),
            "Actions": len(actions),
            "States": len(states),
            "Use Cases": len(use_cases),
            "Verification Cases": len(verifications),
            "Analysis Cases": len(analyses),
            "Views": len(views),
            "Relationships": len(relationships)
        }
        
        total_elements = sum(element_counts.values())
        
        print(f"\nTotal elements created: {total_elements}")
        print("\nElement breakdown:")
        for elem_type, count in element_counts.items():
            if count > 0:
                print(f"  {elem_type}: {count}")
        
        # 查询并验证创建的元素
        print("\n=== Verifying Created Elements ===")
        response = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits",
            headers=headers
        )
        
        if response.status_code == 200:
            commits = response.json()
            print(f"Total commits: {len(commits)}")
            
            if commits:
                latest_commit = commits[0]
                commit_id = latest_commit["@id"]
                
                response = requests.get(
                    f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/elements",
                    headers=headers
                )
                
                if response.status_code == 200:
                    elements = response.json()
                    print(f"Elements in latest commit: {len(elements)}")
        
        print("\n✅ Complete brake system model created successfully!")
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()