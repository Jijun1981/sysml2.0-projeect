# SysML v2 M1 领域模型（UAV 最小闭环）

更新编号：DM-UAV-1

## 1. 目标与范围
- 目标：在 M1 层构建一套“最小但闭环”的无人机（UAV）领域模型，用于反推业务层能力需求；M2 复用官方（pilot）不自造。
- 范围：需求（Requirement）、结构（Part/Port/Connection）、约束（Constraint）、追溯（Trace/Allocation/Satisfies），覆盖最小建模→分配→验证→查询闭环。

## 2. 元模型选取（M1 实例层）
- Requirement：需求定义（标识、文本、度量目标）。
- PartDefinition / PartUsage：结构定义与实例（系统/子系统/部件）。
- PortUsage（简化为 Port）：端口（方向：in/out；类别：power/data）。
- ConnectionUsage（简化为 Connection）：端口间连接（同类别连接）。
- ConstraintUsage（简化为 Constraint）：约束表达（表达式/参数）。
- AllocationUsage（简化为 Allocation）：需求到部件的分配。
- Satisfies（简化为 Trace.Satisfies）：部件满足需求的追溯关系。
- ValueUsage（简化为 Property/Value）：关键参数（电压、电流、功率、能量、重量等）。

> 注：命名与语义贴近官方 SysML v2 概念，M2/细节实现复用 pilot，不在本文展开。

## 3. UAV 最小闭环模型（候选草案）
### 3.1 需求
- R1（续航）：飞行时间 ≥ 20 分钟。
- R2（负载）：有效载荷 ≥ 0.5 千克。

### 3.2 结构
- UAV（Part）
  - Battery（电池，属性：voltage[V]、capacity[Wh]、maxCurrent[A]、mass[kg]）
  - FlightController（飞控，属性：mass[kg]、power[W]）
  - MotorGroup（4×电机+桨组合，聚合属性：count=4、perMotorPower[W]、mass[kg]）
  - PowerBus（电源母线）

### 3.3 端口与连接
- Battery.power_out: Port(type=power, direction=out)
- PowerBus.power_in: Port(type=power, direction=in)
- MotorGroup.power_in: Port(type=power, direction=in)
- FlightController.power_in: Port(type=power, direction=in)
- 连接：Battery.power_out → PowerBus.power_in；PowerBus → MotorGroup.power_in；PowerBus → FlightController.power_in

### 3.4 关键参数（Value）与派生
- totalPropulsionPower[W] = MotorGroup.count × perMotorPower
- hotelLoad[W] = FlightController.power
- totalPower[W] = totalPropulsionPower + hotelLoad
- energy[Wh] = Battery.capacity
- endurance[min] = 60 × energy / totalPower

### 3.5 约束（Constraint）
- C1：endurance[min] ≥ 20
- C2：totalCurrent[A] = totalPower / Battery.voltage；totalCurrent ≤ Battery.maxCurrent
- C3（可选）：全机质量 ≤ 2.5kg（Battery.mass + MotorGroup.mass + FlightController.mass + Payload.mass）

### 3.6 追溯与分配（Trace/Allocation）
- Allocation：R1 → {Battery, MotorGroup, FlightController}
- Satisfies：{Battery, MotorGroup, FlightController} ⟶ R1
- Allocation：R2 → {UAV（或 Payload 子系统）}

### 3.7 最小闭环流程
1) 定义需求 R1/R2
2) 定义结构与层次 UAV → Battery/MotorGroup/FlightController/PowerBus
3) 定义端口与连接（电源供电链路）
4) 录入属性值（电压、容量、功率等）
5) 计算派生值（endurance、totalPower、totalCurrent）
6) 校验约束（C1/C2/可选C3）
7) 建立追溯/分配（R1/R2）
8) 产出验证与覆盖率报告（最小报表）

## 4. 典型数据点（示例值，用于校验闭环）
- Battery: voltage=22.2V、capacity=222Wh、maxCurrent=20A、mass=0.5kg
- MotorGroup: count=4、perMotorPower=80W、mass=0.8kg
- FlightController: power=5W、mass=0.1kg
- 推导：
  - totalPropulsionPower=4×80=320W；hotelLoad=5W；totalPower=325W
  - endurance=60×222/325≈41.0min（满足 R1）
  - totalCurrent=325/22.2≈14.6A ≤ 20A（满足 C2）

## 5. 对业务层的最小能力需求（预告，后续 T13 细化）
- 结构建模：创建 Part/Port/Connection，维护层次与连接合法性（power/data 类别匹配）。
- 需求管理：创建/更新/删除需求，分配/取消分配。
- 参数管理：设置/查询属性值，支持派生值计算（同步或按需触发）。
- 约束校验：按模型计算并返回通过/失败与明细。
- 追溯矩阵：输出需求-部件的覆盖与满足关系汇总。
- 查询能力：按类型/层次/连接拓扑检索；订阅模型变更（最小事件集）。

## 6. 与官方生态的衔接
- M2 与语义：复用 `sysml-v2-pilot` 提供的 KerML/SysML 类型与工具（Adapter/Util/Factory）。
- 表达与接口：继续采用 GraphQL（系统内），必要时参考 `SysML-v2-API-Services` 的资源语义以保证一致性。

---
说明：本文件用于“先建模，后反推业务层”的输入。若你认可该最小闭环，我们将在 T12/T13 产出“案例说明”与“业务层接口清单（契约草案）”。

附：关于“PortUsage（简化为 Port）”的考虑
- 在 SysML v2 中，端口通常以 Usage 形式出现在具体部件的上下文内（即作为某个 Part 的用法/成员）。
- 为了在 M1 最小闭环里降低语义负担与实现复杂度，我们将“端口”在文档表达上简写为 `Port`，但其语义等价于“在具体 Part 上声明的 PortUsage”。
- 也即：我们仍然遵循“Definition/Usage”二分；端口的定义（PortDefinition）可以复用官方 M2；在 M1 实例化/装配时主要关心 Usage 层（端口在具体部件实例上的存在与连接）。
- 这样做的好处：
  1) 文档更易读，便于业务侧讨论连接合法性（power/data、in/out）；
  2) 实现上仍可严格复用 pilot 的 Definition/Usage 体系，不会破坏一致性；
  3) 后续若需要抽象复用（同类端口复用），可自然回到 PortDefinition 层而不影响现有 M1 表达。
