#!/bin/bash
# GitHub认证配置脚本
# 使用方法: ./setup-github-auth.sh YOUR_USERNAME YOUR_TOKEN

if [ $# -ne 2 ]; then
    echo "使用方法: ./setup-github-auth.sh <GitHub用户名> <Personal Access Token>"
    echo "例如: ./setup-github-auth.sh Jijun1981 ghp_xxxxxxxxxxxx"
    exit 1
fi

USERNAME=$1
TOKEN=$2

echo "配置GitHub认证..."
git remote set-url origin https://${USERNAME}:${TOKEN}@github.com/Jijun1981/sysml2.0-projeect.git

echo "测试连接..."
git remote -v

echo "推送代码..."
git push origin feat/cdo-infra-initial-import

echo "完成！"