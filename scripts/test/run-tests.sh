#!/bin/bash

# 测试运行脚本

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

echo "========================================="
echo "  SysML v2 建模平台 - 测试套件"
echo "========================================="

cd "$PROJECT_ROOT/server"

# 单元测试
echo ""
echo "运行单元测试..."
./gradlew test

# 架构测试
echo ""
echo "运行架构测试（循环依赖检查）..."
./gradlew architectureTest

# 性能测试（可选）
if [ "$1" == "--perf" ]; then
    echo ""
    echo "运行性能测试..."
    echo "  Small数据集..."
    ./gradlew performanceTest -Ddataset=small
    
    echo "  Medium数据集..."
    ./gradlew performanceTest -Ddataset=medium
    
    echo "  Large数据集..."
    ./gradlew performanceTest -Ddataset=large
fi

# 生成测试报告
echo ""
echo "生成测试报告..."
./gradlew testReport

echo ""
echo "========================================="
echo "  测试完成"
echo "========================================="
echo "  报告位置: $PROJECT_ROOT/server/build/reports/tests"
echo "========================================="