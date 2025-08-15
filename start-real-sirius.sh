#!/bin/bash

echo "=============================================="
echo "启动真实的Sirius Web应用"
echo "=============================================="

# 设置环境变量
export SIRIUS_WEB_HOME=/mnt/d/sysml2/opensource/sirius-web
export FRONTEND_DIR=$SIRIUS_WEB_HOME/packages/sirius-web/frontend/sirius-web
export BACKEND_DIR=$SIRIUS_WEB_HOME/packages/sirius-web/backend

# 1. 检查PostgreSQL
echo "1. 检查PostgreSQL数据库..."
if ! pg_isready -h localhost -p 5432 -q; then
    echo "❌ PostgreSQL未运行，请先启动数据库"
    exit 1
fi
echo "✅ PostgreSQL已运行"

# 2. 前端已经在运行 (port 5173)
echo ""
echo "2. Sirius Web前端状态..."
if curl -s http://localhost:5173 > /dev/null; then
    echo "✅ 前端已在运行: http://localhost:5173"
else
    echo "⚠️ 前端未运行，请在新终端运行: cd $FRONTEND_DIR && npm start"
fi

# 3. 启动我们的CDO后端服务
echo ""
echo "3. 启动CDO集成后端..."
cd /mnt/d/sysml2/server
if ! curl -s http://localhost:8080/health > /dev/null; then
    echo "启动CDO后端服务..."
    ./gradlew bootRun --args='--spring.profiles.active=dev' &
    CDO_PID=$!
    
    # 等待服务启动
    for i in {1..30}; do
        if curl -s http://localhost:8080/health > /dev/null; then
            echo "✅ CDO后端已启动"
            break
        fi
        echo "等待CDO服务启动... ($i/30)"
        sleep 2
    done
else
    echo "✅ CDO后端已在运行"
fi

echo ""
echo "=============================================="
echo "Sirius Web应用已启动！"
echo "=============================================="
echo ""
echo "访问地址："
echo "🎨 Sirius Web界面: http://localhost:5173"
echo "🔧 CDO后端API: http://localhost:8080"
echo "📊 健康检查: http://localhost:8080/health"
echo "🔌 GraphQL: http://localhost:8080/graphql"
echo ""
echo "功能说明："
echo "- 拖放创建SysML元素（需求、部件、端口）"
echo "- 实时保存到CDO存储库"
echo "- M2元模型操作支持"
echo "- 多用户协作编辑"
echo ""
echo "按Ctrl+C停止所有服务"

# 保持运行
trap "echo '停止服务...'; kill $CDO_PID 2>/dev/null; exit" INT
wait