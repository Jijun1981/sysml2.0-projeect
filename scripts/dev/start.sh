#!/bin/bash

# SysML v2 建模平台开发环境启动脚本

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

echo "========================================="
echo "  SysML v2 建模平台 - 开发环境启动"
echo "========================================="

# 检查Java版本
echo "检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java，请安装Java 17+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "错误: 需要Java 17+，当前版本: $JAVA_VERSION"
    exit 1
fi
echo "✓ Java版本检查通过"

# 创建必要目录
echo "创建数据目录..."
mkdir -p "$PROJECT_ROOT/data/cdo"
mkdir -p "$PROJECT_ROOT/data/app"
mkdir -p "$PROJECT_ROOT/logs"

# 清理旧进程
echo "清理旧进程..."
if [ -f "$PROJECT_ROOT/.pid" ]; then
    OLD_PID=$(cat "$PROJECT_ROOT/.pid")
    if ps -p $OLD_PID > /dev/null; then
        echo "停止旧进程 (PID: $OLD_PID)"
        kill $OLD_PID
        sleep 2
    fi
fi

# 构建项目
echo "构建项目..."
cd "$PROJECT_ROOT/server"
if [ -f gradlew ]; then
    ./gradlew build -x test
else
    echo "警告: gradlew未找到，跳过构建"
fi

# 启动Spring Boot应用
echo "启动Spring Boot应用..."
cd "$PROJECT_ROOT/server"
nohup java -jar build/libs/sysml-platform-1.0.0.jar \
    --spring.profiles.active=dev \
    > "$PROJECT_ROOT/logs/server.log" 2>&1 &

SERVER_PID=$!
echo $SERVER_PID > "$PROJECT_ROOT/.pid"

echo "服务启动中 (PID: $SERVER_PID)..."

# 等待服务启动
MAX_WAIT=30
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if curl -s http://localhost:8080/health > /dev/null 2>&1; then
        echo ""
        echo "✓ 服务启动成功!"
        break
    fi
    echo -n "."
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done

if [ $WAIT_COUNT -eq $MAX_WAIT ]; then
    echo ""
    echo "错误: 服务启动超时"
    echo "查看日志: tail -f $PROJECT_ROOT/logs/server.log"
    exit 1
fi

# 显示服务信息
echo ""
echo "========================================="
echo "  服务已启动"
echo "========================================="
echo "  Health Check: http://localhost:8080/health"
echo "  GraphQL:      http://localhost:8080/graphql"
echo "  GraphiQL:     http://localhost:8080/graphiql"
echo "  Metrics:      http://localhost:8080/actuator/metrics"
echo ""
echo "  查看日志: tail -f $PROJECT_ROOT/logs/server.log"
echo "  停止服务: $SCRIPT_DIR/stop.sh"
echo "========================================="