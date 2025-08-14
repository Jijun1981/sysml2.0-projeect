# SysML v2 汽车制动系统验证项目执行指南

## 项目概述

本项目基于官方SysML-v2-API-Services创建一个完整的汽车制动系统验证模型，验证SysML 2.0的建模能力，为后续自研提供技术参考。

## 快速开始

### 第一阶段：环境搭建 (1周)

#### 1. 部署SysML-v2-API-Services

```bash
# 克隆官方仓库
git clone https://github.com/Systems-Modeling/SysML-v2-API-Services.git
cd SysML-v2-API-Services

# 使用Docker Compose部署
docker-compose up -d

# 验证服务状态
curl http://localhost:9000/health
```

#### 2. 配置开发环境

```bash
# 安装Python测试依赖
pip install pytest requests jsonschema pytest-html

# 配置Postman或创建API测试脚本
# 导入API端点配置
```

#### 验收标准
- [ ] API服务在http://localhost:9000正常运行
- [ ] 数据库连接正常
- [ ] Swagger文档可访问 (http://localhost:9000/docs)
- [ ] 基础API调用成功

### 第二阶段：需求建模 (2周)

#### 1. 创建项目和初始提交

```python
# 创建测试项目
import requests

# 创建项目
project_data = {
    "name": "BrakingSystemValidation",
    "description": "汽车制动系统SysML验证项目"
}
response = requests.post("http://localhost:9000/projects", json=project_data)
project_id = response.json()["@id"]

# 创建初始提交
commit_data = {"comment": "初始化制动系统模型"}
response = requests.post(f"http://localhost:9000/projects/{project_id}/commits", json=commit_data)
commit_id = response.json()["@id"]
```

#### 2. 创建功能需求

```python
# 制动距离需求
braking_distance_req = {
    "@type": "RequirementDefinition",
    "declaredName": "BrakingDistanceRequirement",
    "text": "车辆必须在100km/h速度下40米内完全停止",
    "reqId": "REQ-FUNC-001"
}

response = requests.post(
    f"http://localhost:9000/projects/{project_id}/commits/{commit_id}/elements",
    json=braking_distance_req
)
```

#### 3. 创建性能需求

```python
# 制动力需求
braking_force_req = {
    "@type": "RequirementDefinition", 
    "declaredName": "BrakingForceRequirement",
    "text": "制动力必须达到车重的0.8倍以上",
    "reqId": "REQ-PERF-001"
}

# 响应时间需求
response_time_req = {
    "@type": "RequirementDefinition",
    "declaredName": "ResponseTimeRequirement", 
    "text": "制动系统响应时间不得超过150毫秒",
    "reqId": "REQ-FUNC-002"
}
```

#### 验收标准
- [ ] 功能需求(3个)完整创建
- [ ] 性能需求(2个)完整创建  
- [ ] 约束需求(2个)完整创建
- [ ] 需求层次关系正确
- [ ] 需求文本描述清晰

### 第三阶段：结构建模 (2周)

#### 1. 创建系统级部件

```python
# 制动系统总成
braking_system = {
    "@type": "PartDefinition",
    "declaredName": "BrakingSystem", 
    "documentation": "汽车制动系统总成，包含所有制动相关组件"
}

response = requests.post(
    f"http://localhost:9000/projects/{project_id}/commits/{commit_id}/elements",
    json=braking_system
)
braking_system_id = response.json()["@id"]
```

#### 2. 创建主要组件

```python
# 制动踏板
brake_pedal = {
    "@type": "PartDefinition",
    "declaredName": "BrakePedal",
    "documentation": "制动踏板组件，接收驾驶员制动指令"
}

# 制动主缸
master_cylinder = {
    "@type": "PartDefinition", 
    "declaredName": "MasterCylinder",
    "documentation": "制动主缸，将踏板力转换为液压力"
}

# 制动盘
brake_disc = {
    "@type": "PartDefinition",
    "declaredName": "BrakeDisc", 
    "documentation": "制动盘，与制动片摩擦产生制动力"
}

# 制动卡钳
brake_caliper = {
    "@type": "PartDefinition",
    "declaredName": "BrakeCaliper",
    "documentation": "制动卡钳，夹紧制动片"
}

# ABS控制器
abs_controller = {
    "@type": "PartDefinition",
    "declaredName": "ABSController",
    "documentation": "ABS控制单元，防止车轮抱死"
}
```

#### 3. 建立组装关系

```python
# 创建部件使用关系
brake_pedal_usage = {
    "@type": "PartUsage",
    "declaredName": "brakePedalUsage",
    # 需要正确设置引用关系
}
```

#### 验收标准
- [ ] 系统级PartDefinition创建成功
- [ ] 5个主要组件PartDefinition创建成功
- [ ] 部件层次结构清晰
- [ ] PartUsage关系正确建立
- [ ] 接口和连接定义完备

### 第四阶段：需求追踪建模 (1周)

#### 1. 创建需求满足关系

```python
# 制动系统满足制动距离需求
satisfy_braking_distance = {
    "@type": "SatisfyRequirementUsage",
    "declaredName": "brakingSystemSatisfiesBrakingDistance",
    # 需要引用需求和满足需求的部件
}

response = requests.post(
    f"http://localhost:9000/projects/{project_id}/commits/{commit_id}/elements",
    json=satisfy_braking_distance
)
```

#### 2. 创建验证用例

```python
# 制动距离验证用例
braking_distance_verification = {
    "@type": "VerificationCaseDefinition",
    "declaredName": "BrakingDistanceVerification",
    "text": "在测试场地以100km/h速度进行制动测试，测量制动距离"
}
```

#### 验收标准
- [ ] 所有需求与对应部件的满足关系建立
- [ ] 验证用例定义完整
- [ ] 需求追踪矩阵可生成
- [ ] 验证方法清晰定义

### 第五阶段：集成测试和验证 (1周)

#### 1. 执行完整工作流测试

```python
# 端到端测试脚本
def test_complete_modeling_workflow():
    # 1. 创建项目
    project = create_project()
    
    # 2. 创建需求
    requirements = create_requirements(project)
    
    # 3. 创建部件
    parts = create_parts(project)
    
    # 4. 建立关系
    relationships = create_relationships(project, requirements, parts)
    
    # 5. 验证完整性
    validate_model_completeness(project)
    
    assert len(requirements) == 7  # 总需求数
    assert len(parts) == 6         # 总部件数  
    assert len(relationships) > 0   # 关系数量
```

#### 2. 性能和稳定性测试

```python
def test_performance():
    # 批量创建元素测试
    start_time = time.time()
    
    for i in range(100):
        create_requirement(f"TestReq_{i}")
        
    end_time = time.time()
    avg_time = (end_time - start_time) / 100
    
    assert avg_time < 0.5  # 平均响应时间小于500ms
```

#### 验收标准
- [ ] 端到端工作流100%成功
- [ ] 性能指标满足要求
- [ ] 数据一致性验证通过
- [ ] 错误处理机制验证

## 关键API调用示例

### 1. 项目管理

```bash
# 创建项目
curl -X POST http://localhost:9000/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "BrakingSystemValidation", "description": "汽车制动系统验证"}'

# 获取项目列表
curl http://localhost:9000/projects
```

### 2. 元素创建

```bash
# 创建需求定义
curl -X POST http://localhost:9000/projects/{projectId}/commits/{commitId}/elements \
  -H "Content-Type: application/json" \
  -d '{
    "@type": "RequirementDefinition",
    "declaredName": "BrakingDistanceRequirement", 
    "text": "车辆必须在100km/h速度下40米内完全停止"
  }'

# 创建部件定义  
curl -X POST http://localhost:9000/projects/{projectId}/commits/{commitId}/elements \
  -H "Content-Type: application/json" \
  -d '{
    "@type": "PartDefinition",
    "declaredName": "BrakingSystem",
    "documentation": "汽车制动系统总成"
  }'
```

### 3. 查询和验证

```bash
# 查询所有元素
curl http://localhost:9000/projects/{projectId}/commits/{commitId}/elements

# 按类型查询
curl "http://localhost:9000/projects/{projectId}/commits/{commitId}/elements?@type=RequirementDefinition"
```

## 预期输出结果

### 1. 完整的M1模型结构

```
BrakingSystem (系统总成)
├── 需求层 (7个需求)
│   ├── 功能需求 (3个)
│   │   ├── 制动距离需求
│   │   ├── 响应时间需求  
│   │   └── 安全性需求
│   ├── 性能需求 (2个)
│   │   ├── 制动力需求
│   │   └── 耐久性需求
│   └── 约束需求 (2个)
│       ├── 重量约束
│       └── 成本约束
├── 结构层 (6个部件)
│   ├── BrakingSystem (总成)
│   ├── BrakePedal (制动踏板)
│   ├── MasterCylinder (主缸)
│   ├── BrakeDisc (制动盘)
│   ├── BrakeCaliper (卡钳)
│   └── ABSController (ABS控制器)
└── 关系层
    ├── 需求满足关系 (7个)
    ├── 部件使用关系 (5个)
    └── 验证关系 (7个)
```

### 2. JSON数据示例

```json
{
  "@type": "RequirementDefinition",
  "@id": "req_001",
  "declaredName": "BrakingDistanceRequirement",
  "text": "车辆必须在100km/h速度下40米内完全停止",
  "reqId": "REQ-FUNC-001"
}
```

## 常见问题和解决方案

### 1. API连接问题

**问题**: 无法连接到API服务
**解决**: 检查Docker服务状态，确认端口9000未被占用

```bash
docker ps
netstat -tulpn | grep 9000
```

### 2. 元素创建失败

**问题**: 创建SysML元素返回400错误
**解决**: 检查JSON格式和必需字段

```python
# 确保包含必需字段
element = {
    "@type": "RequirementDefinition",  # 必需
    "declaredName": "MyRequirement"    # 必需
}
```

### 3. 关系建立失败

**问题**: 无法创建satisfy关系
**解决**: 确认引用的元素存在且ID正确

```python
# 先查询元素是否存在
response = requests.get(f"http://localhost:9000/projects/{project_id}/commits/{commit_id}/elements/{element_id}")
if response.status_code == 200:
    # 元素存在，可以创建关系
    pass
```

## 项目交付物

### 1. 完整的SysML模型
- 需求模型 (RequirementDefinition)
- 结构模型 (PartDefinition + PartUsage)  
- 关系模型 (SatisfyRequirementUsage)
- 验证模型 (VerificationCaseDefinition)

### 2. API测试套件
- 功能测试用例 (100个+)
- 性能测试用例 (10个+)
- 集成测试用例 (5个+)
- 测试报告和分析

### 3. 技术文档
- SysML 2.0建模最佳实践
- API使用经验总结
- 建模工具链评估
- 后续自研建议

### 4. 代码和配置
- API测试脚本
- Docker部署配置
- 数据备份和恢复脚本
- 持续集成配置

这个验证项目将为您提供对SysML 2.0和官方API服务的深入理解，为后续自研系统提供宝贵的技术参考和实践经验。