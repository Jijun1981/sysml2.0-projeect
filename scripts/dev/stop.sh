#!/bin/bash

# 停止开发环境脚本

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

echo "停止SysML v2建模平台..."

if [ -f "$PROJECT_ROOT/.pid" ]; then
    PID=$(cat "$PROJECT_ROOT/.pid")
    if ps -p $PID > /dev/null; then
        echo "停止进程 (PID: $PID)"
        kill $PID
        sleep 2
        
        # 强制停止
        if ps -p $PID > /dev/null; then
            echo "强制停止进程..."
            kill -9 $PID
        fi
        
        rm "$PROJECT_ROOT/.pid"
        echo "✓ 服务已停止"
    else
        echo "进程不存在 (PID: $PID)"
        rm "$PROJECT_ROOT/.pid"
    fi
else
    echo "未找到PID文件"
fi

# 清理端口占用
echo "检查端口占用..."
if lsof -i:8080 > /dev/null 2>&1; then
    echo "清理8080端口..."
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
fi

if lsof -i:2036 > /dev/null 2>&1; then
    echo "清理2036端口(CDO)..."
    lsof -ti:2036 | xargs kill -9 2>/dev/null || true
fi

echo "✓ 停止完成"