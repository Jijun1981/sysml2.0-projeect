#!/bin/bash

# 真实的Sirius Web后端启动脚本
# 使用真实的Spring Boot应用，不是mock

echo "启动真实的Sirius Web后端服务..."

# 设置环境变量
# export GITHUB_TOKEN=<your_token_here>
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8080

# 编译Java代码
cd /mnt/d/sysml2/server
javac -cp "lib/*:target/classes" src/main/java/com/sysml/platform/*.java -d target/classes

# 运行Spring Boot应用
java -cp "lib/*:target/classes:src/main/resources" \
     -Dspring.profiles.active=dev \
     -Dserver.port=8080 \
     com.sysml.platform.SysMLPlatformApplication