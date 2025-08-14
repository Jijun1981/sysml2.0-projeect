#!/usr/bin/env python3
"""
以Part为中心提取制动系统模型数据，展示所有关联关系
"""
import requests
import json
from datetime import datetime

BASE_URL = "http://localhost:9000"
headers = {"Content-Type": "application/json"}

def get_project_data(project_id):
    """获取项目的所有数据"""
    # 1. 获取所有commits
    response = requests.get(f"{BASE_URL}/projects/{project_id}/commits", headers=headers)
    if response.status_code != 200:
        return None
    
    commits = response.json()
    
    # 2. 收集所有元素
    all_elements = {}
    elements_by_type = {}
    
    for commit in commits:
        commit_id = commit["@id"]
        elements = requests.get(
            f"{BASE_URL}/projects/{project_id}/commits/{commit_id}/elements",
            headers=headers
        ).json()
        
        for elem in elements:
            elem_id = elem.get("@id")
            elem_type = elem.get("@type")
            elem_name = elem.get("name", "unnamed")
            
            if elem_id:
                all_elements[elem_id] = {
                    "id": elem_id,
                    "type": elem_type,
                    "name": elem_name,
                    "data": elem
                }
                
                if elem_type not in elements_by_type:
                    elements_by_type[elem_type] = []
                elements_by_type[elem_type].append(elem_id)
    
    return all_elements, elements_by_type

def build_part_centric_model(all_elements, elements_by_type):
    """构建以Part为中心的关系模型"""
    
    model = {
        "projectId": "ca54e418-f2e2-462b-b097-293358195138",
        "projectName": "CompleteBrakeSystem",
        "timestamp": datetime.now().isoformat(),
        "parts": {},
        "requirements": {},
        "connections": {},
        "satisfyRelationships": {},
        "allocations": {},
        "constraints": {},
        "interfaces": {},
        "ports": {},
        "actions": {},
        "states": {},
        "summary": {}
    }
    
    # 1. 收集所有Parts
    part_definitions = elements_by_type.get("PartDefinition", [])
    part_usages = elements_by_type.get("PartUsage", [])
    
    for part_id in part_definitions:
        part = all_elements[part_id]
        model["parts"][part_id] = {
            "id": part_id,
            "name": part["name"],
            "type": "PartDefinition",
            "relatedPorts": [],
            "relatedConnections": [],
            "satisfiedRequirements": [],
            "allocatedRequirements": [],
            "appliedConstraints": [],
            "associatedActions": [],
            "states": []
        }
    
    for part_usage_id in part_usages:
        part_usage = all_elements[part_usage_id]
        model["parts"][part_usage_id] = {
            "id": part_usage_id,
            "name": part_usage["name"],
            "type": "PartUsage",
            "definitionRef": None,  # 可以通过type关系找到
            "relatedPorts": [],
            "relatedConnections": [],
            "satisfiedRequirements": [],
            "allocatedRequirements": [],
            "appliedConstraints": []
        }
    
    # 2. 收集Requirements并关联到Parts
    req_definitions = elements_by_type.get("RequirementDefinition", [])
    for req_id in req_definitions:
        req = all_elements[req_id]
        model["requirements"][req_id] = {
            "id": req_id,
            "name": req["name"],
            "type": "RequirementDefinition",
            "text": req["data"].get("text", []),
            "reqId": req["data"].get("reqId", ""),
            "satisfiedByParts": [],
            "allocatedToParts": []
        }
    
    # 3. 收集Ports并关联到Parts
    port_definitions = elements_by_type.get("PortDefinition", [])
    port_usages = elements_by_type.get("PortUsage", [])
    
    for port_id in port_definitions + port_usages:
        port = all_elements[port_id]
        port_type = "PortDefinition" if port_id in port_definitions else "PortUsage"
        model["ports"][port_id] = {
            "id": port_id,
            "name": port["name"],
            "type": port_type,
            "owningPart": None,  # 需要通过关系确定
            "connectedPorts": []
        }
    
    # 4. 收集Connections并关联Parts
    connections = elements_by_type.get("ConnectionUsage", [])
    connectors = elements_by_type.get("BindingConnector", [])
    
    for conn_id in connections + connectors:
        conn = all_elements[conn_id]
        conn_type = conn["type"]
        model["connections"][conn_id] = {
            "id": conn_id,
            "name": conn["name"],
            "type": conn_type,
            "sourcePart": None,
            "targetPart": None,
            "sourcePort": None,
            "targetPort": None
        }
        
        # 根据名称推断连接关系
        conn_name = conn["name"].lower()
        if "pedal" in conn_name and "master" in conn_name:
            # pedalToMasterCylinderConnection
            for part_id, part_info in model["parts"].items():
                if "pedal" in part_info["name"].lower():
                    model["connections"][conn_id]["sourcePart"] = part_id
                    part_info["relatedConnections"].append(conn_id)
                elif "master" in part_info["name"].lower():
                    model["connections"][conn_id]["targetPart"] = part_id
                    part_info["relatedConnections"].append(conn_id)
        elif "master" in conn_name and "caliper" in conn_name:
            # masterCylinderToCaliperConnection
            for part_id, part_info in model["parts"].items():
                if "master" in part_info["name"].lower():
                    model["connections"][conn_id]["sourcePart"] = part_id
                    part_info["relatedConnections"].append(conn_id)
                elif "caliper" in part_info["name"].lower():
                    model["connections"][conn_id]["targetPart"] = part_id
                    part_info["relatedConnections"].append(conn_id)
    
    # 5. 收集Satisfy关系
    satisfy_usages = elements_by_type.get("SatisfyRequirementUsage", [])
    for satisfy_id in satisfy_usages:
        satisfy = all_elements[satisfy_id]
        model["satisfyRelationships"][satisfy_id] = {
            "id": satisfy_id,
            "name": satisfy["name"],
            "satisfyingPart": None,
            "satisfiedRequirement": None
        }
        
        # 根据名称推断满足关系
        satisfy_name = satisfy["name"].lower()
        if "system" in satisfy_name and "functional" in satisfy_name:
            # systemSatisfiesFunctional
            for part_id, part_info in model["parts"].items():
                if "system" in part_info["name"].lower():
                    model["satisfyRelationships"][satisfy_id]["satisfyingPart"] = part_id
                    for req_id, req_info in model["requirements"].items():
                        if "functional" in req_info["name"].lower():
                            model["satisfyRelationships"][satisfy_id]["satisfiedRequirement"] = req_id
                            part_info["satisfiedRequirements"].append(req_id)
                            req_info["satisfiedByParts"].append(part_id)
                            break
                    break
        elif "pedal" in satisfy_name and "response" in satisfy_name:
            # pedalSatisfiesResponseTime
            for part_id, part_info in model["parts"].items():
                if "pedal" in part_info["name"].lower():
                    model["satisfyRelationships"][satisfy_id]["satisfyingPart"] = part_id
                    for req_id, req_info in model["requirements"].items():
                        if "response" in req_info["name"].lower():
                            model["satisfyRelationships"][satisfy_id]["satisfiedRequirement"] = req_id
                            part_info["satisfiedRequirements"].append(req_id)
                            req_info["satisfiedByParts"].append(part_id)
                            break
                    break
        elif "caliper" in satisfy_name and "performance" in satisfy_name:
            # caliperSatisfiesPerformance
            for part_id, part_info in model["parts"].items():
                if "caliper" in part_info["name"].lower():
                    model["satisfyRelationships"][satisfy_id]["satisfyingPart"] = part_id
                    for req_id, req_info in model["requirements"].items():
                        if "performance" in req_info["name"].lower():
                            model["satisfyRelationships"][satisfy_id]["satisfiedRequirement"] = req_id
                            part_info["satisfiedRequirements"].append(req_id)
                            req_info["satisfiedByParts"].append(part_id)
                            break
                    break
    
    # 6. 收集Allocations
    allocation_usages = elements_by_type.get("AllocationUsage", [])
    for alloc_id in allocation_usages:
        alloc = all_elements[alloc_id]
        model["allocations"][alloc_id] = {
            "id": alloc_id,
            "name": alloc["name"],
            "fromElement": None,
            "toElement": None
        }
        
        # 根据名称推断分配关系
        alloc_name = alloc["name"].lower()
        if "functional" in alloc_name and "system" in alloc_name:
            for req_id, req_info in model["requirements"].items():
                if "functional" in req_info["name"].lower():
                    model["allocations"][alloc_id]["fromElement"] = req_id
                    for part_id, part_info in model["parts"].items():
                        if "system" in part_info["name"].lower():
                            model["allocations"][alloc_id]["toElement"] = part_id
                            part_info["allocatedRequirements"].append(req_id)
                            req_info["allocatedToParts"].append(part_id)
                            break
                    break
        elif "performance" in alloc_name and "caliper" in alloc_name:
            for req_id, req_info in model["requirements"].items():
                if "performance" in req_info["name"].lower():
                    model["allocations"][alloc_id]["fromElement"] = req_id
                    for part_id, part_info in model["parts"].items():
                        if "caliper" in part_info["name"].lower():
                            model["allocations"][alloc_id]["toElement"] = part_id
                            part_info["allocatedRequirements"].append(req_id)
                            req_info["allocatedToParts"].append(part_id)
                            break
                    break
    
    # 7. 收集Constraints
    constraint_definitions = elements_by_type.get("ConstraintDefinition", [])
    for const_id in constraint_definitions:
        const = all_elements[const_id]
        model["constraints"][const_id] = {
            "id": const_id,
            "name": const["name"],
            "type": "ConstraintDefinition",
            "appliedToParts": []
        }
        
        # 根据名称推断约束应用
        const_name = const["name"].lower()
        if "pressure" in const_name:
            for part_id, part_info in model["parts"].items():
                if "cylinder" in part_info["name"].lower():
                    model["constraints"][const_id]["appliedToParts"].append(part_id)
                    part_info["appliedConstraints"].append(const_id)
        elif "force" in const_name:
            for part_id, part_info in model["parts"].items():
                if "caliper" in part_info["name"].lower() or "pad" in part_info["name"].lower():
                    model["constraints"][const_id]["appliedToParts"].append(part_id)
                    part_info["appliedConstraints"].append(const_id)
    
    # 8. 收集Interfaces
    interface_definitions = elements_by_type.get("InterfaceDefinition", [])
    for iface_id in interface_definitions:
        iface = all_elements[iface_id]
        model["interfaces"][iface_id] = {
            "id": iface_id,
            "name": iface["name"],
            "type": "InterfaceDefinition",
            "implementedByParts": []
        }
    
    # 9. 收集Actions
    action_definitions = elements_by_type.get("ActionDefinition", [])
    for action_id in action_definitions:
        action = all_elements[action_id]
        model["actions"][action_id] = {
            "id": action_id,
            "name": action["name"],
            "type": "ActionDefinition",
            "performedByParts": []
        }
        
        # 根据名称推断动作执行者
        action_name = action["name"].lower()
        if "brake" in action_name:
            for part_id, part_info in model["parts"].items():
                if "system" in part_info["name"].lower() or "pedal" in part_info["name"].lower():
                    model["actions"][action_id]["performedByParts"].append(part_id)
                    if "associatedActions" in part_info:
                        part_info["associatedActions"].append(action_id)
    
    # 10. 收集States
    state_definitions = elements_by_type.get("StateDefinition", [])
    for state_id in state_definitions:
        state = all_elements[state_id]
        model["states"][state_id] = {
            "id": state_id,
            "name": state["name"],
            "type": "StateDefinition",
            "applicableToParts": []
        }
        
        # 所有状态都适用于制动系统
        for part_id, part_info in model["parts"].items():
            if "system" in part_info["name"].lower():
                model["states"][state_id]["applicableToParts"].append(part_id)
                if "states" in part_info:
                    part_info["states"].append(state_id)
    
    # 11. 生成统计摘要
    model["summary"] = {
        "totalParts": len(model["parts"]),
        "totalRequirements": len(model["requirements"]),
        "totalConnections": len(model["connections"]),
        "totalPorts": len(model["ports"]),
        "totalConstraints": len(model["constraints"]),
        "totalInterfaces": len(model["interfaces"]),
        "totalActions": len(model["actions"]),
        "totalStates": len(model["states"]),
        "satisfyRelationships": len(model["satisfyRelationships"]),
        "allocations": len(model["allocations"]),
        "partsWithRequirements": sum(1 for p in model["parts"].values() if p.get("satisfiedRequirements") or p.get("allocatedRequirements")),
        "partsWithConnections": sum(1 for p in model["parts"].values() if p.get("relatedConnections")),
        "partsWithConstraints": sum(1 for p in model["parts"].values() if p.get("appliedConstraints"))
    }
    
    return model

def main():
    project_id = "ca54e418-f2e2-462b-b097-293358195138"
    
    print("=" * 80)
    print("提取以Part为中心的制动系统模型数据")
    print("=" * 80)
    
    # 获取所有数据
    print("\n1. 获取项目数据...")
    all_elements, elements_by_type = get_project_data(project_id)
    
    if not all_elements:
        print("无法获取项目数据")
        return
    
    print(f"   获取到 {len(all_elements)} 个元素")
    print(f"   包含 {len(elements_by_type)} 种元素类型")
    
    # 构建Part为中心的模型
    print("\n2. 构建Part为中心的关系模型...")
    model = build_part_centric_model(all_elements, elements_by_type)
    
    # 保存为JSON文件
    output_file = "/mnt/d/sysml2/opensource/brake_system_part_centric_model.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
    
    print(f"\n3. 模型已保存到: {output_file}")
    
    # 打印摘要
    print("\n4. 模型摘要:")
    print(f"   - Parts总数: {model['summary']['totalParts']}")
    print(f"   - Requirements总数: {model['summary']['totalRequirements']}")
    print(f"   - Connections总数: {model['summary']['totalConnections']}")
    print(f"   - Ports总数: {model['summary']['totalPorts']}")
    print(f"   - Constraints总数: {model['summary']['totalConstraints']}")
    print(f"   - 满足关系数: {model['summary']['satisfyRelationships']}")
    print(f"   - 分配关系数: {model['summary']['allocations']}")
    print(f"   - 有需求关联的Parts: {model['summary']['partsWithRequirements']}")
    print(f"   - 有连接关系的Parts: {model['summary']['partsWithConnections']}")
    print(f"   - 有约束的Parts: {model['summary']['partsWithConstraints']}")
    
    # 打印部分Part详情
    print("\n5. 关键Part的关联关系示例:")
    
    # 找出关系最丰富的几个Parts
    parts_with_relations = []
    for part_id, part_info in model["parts"].items():
        relation_count = (
            len(part_info.get("satisfiedRequirements", [])) +
            len(part_info.get("allocatedRequirements", [])) +
            len(part_info.get("relatedConnections", [])) +
            len(part_info.get("appliedConstraints", []))
        )
        if relation_count > 0:
            parts_with_relations.append((part_id, part_info, relation_count))
    
    parts_with_relations.sort(key=lambda x: x[2], reverse=True)
    
    for part_id, part_info, rel_count in parts_with_relations[:3]:
        print(f"\n   Part: {part_info['name']} ({part_info['type']})")
        print(f"   ID: {part_id}")
        
        if part_info.get("satisfiedRequirements"):
            print(f"   满足的需求:")
            for req_id in part_info["satisfiedRequirements"]:
                req = model["requirements"].get(req_id)
                if req:
                    print(f"     - {req['name']} ({req.get('reqId', 'N/A')})")
        
        if part_info.get("allocatedRequirements"):
            print(f"   分配的需求:")
            for req_id in part_info["allocatedRequirements"]:
                req = model["requirements"].get(req_id)
                if req:
                    print(f"     - {req['name']} ({req.get('reqId', 'N/A')})")
        
        if part_info.get("relatedConnections"):
            print(f"   相关连接:")
            for conn_id in part_info["relatedConnections"]:
                conn = model["connections"].get(conn_id)
                if conn:
                    print(f"     - {conn['name']}")
        
        if part_info.get("appliedConstraints"):
            print(f"   应用的约束:")
            for const_id in part_info["appliedConstraints"]:
                const = model["constraints"].get(const_id)
                if const:
                    print(f"     - {const['name']}")
    
    print("\n" + "=" * 80)
    print("✅ Part为中心的模型数据提取完成!")
    print("=" * 80)

if __name__ == "__main__":
    main()