#!/bin/bash

echo "========================================="
echo "Starting Sirius Web with CDO Integration"
echo "========================================="

# 1. 启动PostgreSQL（如果需要）
echo "1. Checking PostgreSQL..."
if ! pg_isready -h localhost -p 5432 -U sysml_user -d sysml_dev_db; then
    echo "PostgreSQL not running. Please start it first."
    exit 1
fi
echo "✅ PostgreSQL is running"

# 2. 启动CDO后端服务
echo ""
echo "2. Starting CDO Backend Service..."
cd /mnt/d/sysml2/server
./gradlew bootRun &
CDO_PID=$!
echo "✅ CDO Backend started (PID: $CDO_PID)"

# 等待CDO服务就绪
echo "Waiting for CDO service to be ready..."
sleep 10
until curl -s http://localhost:8080/health > /dev/null; do
    echo "Waiting for CDO service..."
    sleep 2
done
echo "✅ CDO service is ready"

# 3. 启动Sirius Web前端
echo ""
echo "3. Starting Sirius Web Frontend..."
cd /mnt/d/sysml2/opensource/sirius-web/packages/sirius-web/frontend/sirius-web
npm start &
SIRIUS_PID=$!
echo "✅ Sirius Web Frontend started (PID: $SIRIUS_PID)"

echo ""
echo "========================================="
echo "All services started successfully!"
echo "========================================="
echo ""
echo "Access Points:"
echo "- Sirius Web UI: http://localhost:5173"
echo "- CDO Backend API: http://localhost:8080"
echo "- Health Check: http://localhost:8080/health"
echo "- GraphQL Playground: http://localhost:8080/graphql"
echo ""
echo "Press Ctrl+C to stop all services"

# 等待用户中断
trap "echo 'Stopping services...'; kill $CDO_PID $SIRIUS_PID; exit" INT
wait