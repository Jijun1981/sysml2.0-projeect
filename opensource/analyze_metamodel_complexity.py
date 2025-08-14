#!/usr/bin/env python3
"""
分析SysML v2元模型的复杂度
"""
import os
import re
from pathlib import Path

def analyze_java_interface(file_path):
    """分析Java接口文件"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 提取接口名
    interface_match = re.search(r'public interface (\w+)', content)
    if not interface_match:
        return None
    
    interface_name = interface_match.group(1)
    
    # 提取继承的接口
    extends_match = re.search(r'public interface \w+ extends ([^{]+)', content)
    extends_list = []
    if extends_match:
        extends_str = extends_match.group(1)
        extends_list = [e.strip() for e in extends_str.split(',')]
    
    # 统计方法数量
    method_count = len(re.findall(r'\s+\w+(?:<[^>]+>)?\s+\w+\([^)]*\);', content))
    
    # 统计集合类型的属性
    collection_methods = len(re.findall(r'List<|Set<|Collection<', content))
    
    return {
        'name': interface_name,
        'extends': extends_list,
        'method_count': method_count,
        'collection_methods': collection_methods
    }

def main():
    metamodel_path = Path("/mnt/d/sysml2/opensource/SysML-v2-API-Services/app/org/omg/sysml/metamodel")
    
    # 只分析接口文件，不包括impl目录
    interface_files = [f for f in metamodel_path.glob("*.java") if f.is_file()]
    
    interfaces = {}
    for file_path in interface_files:
        result = analyze_java_interface(file_path)
        if result:
            interfaces[result['name']] = result
    
    print("=" * 80)
    print("SysML v2 元模型复杂度分析")
    print("=" * 80)
    
    print(f"\n1. 基本统计:")
    print(f"   接口总数: {len(interfaces)}")
    
    # 统计继承深度
    inheritance_stats = {}
    for name, info in interfaces.items():
        extends_count = len(info['extends'])
        if extends_count not in inheritance_stats:
            inheritance_stats[extends_count] = []
        inheritance_stats[extends_count].append(name)
    
    print(f"\n2. 继承关系复杂度:")
    for count in sorted(inheritance_stats.keys()):
        print(f"   继承{count}个接口: {len(inheritance_stats[count])}个")
        if count >= 2:  # 多重继承的例子
            for example in inheritance_stats[count][:3]:
                extends = interfaces[example]['extends']
                print(f"      - {example} extends {', '.join(extends)}")
    
    # 统计方法数量
    method_stats = sorted(interfaces.items(), key=lambda x: x[1]['method_count'], reverse=True)
    
    print(f"\n3. 接口方法数量TOP 10:")
    for name, info in method_stats[:10]:
        print(f"   {name}: {info['method_count']}个方法")
    
    # 分析关键的基础接口
    print(f"\n4. 核心基础接口分析:")
    core_interfaces = ['Element', 'Type', 'Feature', 'Relationship', 'Namespace']
    for name in core_interfaces:
        if name in interfaces:
            info = interfaces[name]
            print(f"\n   {name}:")
            print(f"      继承自: {', '.join(info['extends']) if info['extends'] else 'None'}")
            print(f"      方法数: {info['method_count']}")
            print(f"      集合属性: {info['collection_methods']}")
    
    # 统计总体复杂度
    total_methods = sum(info['method_count'] for info in interfaces.values())
    total_collections = sum(info['collection_methods'] for info in interfaces.values())
    
    print(f"\n5. 总体复杂度:")
    print(f"   所有接口方法总数: {total_methods}")
    print(f"   平均每个接口方法数: {total_methods / len(interfaces):.1f}")
    print(f"   集合类型属性总数: {total_collections}")
    
    # 分析继承层次
    print(f"\n6. 继承层次分析:")
    
    # 找出没有被继承的接口（叶子节点）
    all_extended = set()
    for info in interfaces.values():
        all_extended.update(info['extends'])
    
    leaf_interfaces = [name for name in interfaces.keys() if name not in all_extended]
    print(f"   叶子接口数量: {len(leaf_interfaces)}")
    
    # 找出根接口（不继承任何接口的）
    root_interfaces = [name for name, info in interfaces.items() if not info['extends']]
    print(f"   根接口数量: {len(root_interfaces)}")
    if root_interfaces:
        print(f"   根接口: {', '.join(root_interfaces[:5])}")
    
    print(f"\n7. 复杂度评估:")
    print(f"""
   🔴 高复杂度因素:
   - 183个接口定义，177个实现类
   - 平均每个接口有{total_methods / len(interfaces):.0f}个方法
   - 大量多重继承（最多继承{max(inheritance_stats.keys())}个接口）
   - 错综复杂的交叉引用关系
   - 大量集合类型属性（{total_collections}个）
   
   🟡 维护挑战:
   - 手写所有接口和实现类，容易出错
   - 修改一个基础接口会影响大量子类
   - 没有可视化工具辅助理解继承关系
   - JPA映射配置复杂（每个类都需要注解）
   
   🟢 相比EMF的优势:
   - 不依赖Eclipse生态
   - 更灵活的定制能力
   - 标准Java，易于集成
   - 更好的IDE支持
   """)

if __name__ == "__main__":
    main()