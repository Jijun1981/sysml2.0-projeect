#!/bin/bash

# 配置当前用户sudo免密码脚本
# 警告：这会降低系统安全性，仅在开发环境使用

echo "========================================="
echo "  配置sudo免密码 (用户: $USER)"
echo "========================================="

# 创建sudoers.d配置文件内容
SUDO_CONFIG="$USER ALL=(ALL) NOPASSWD:ALL"

echo ""
echo "需要执行以下步骤："
echo ""
echo "1. 打开新的终端窗口"
echo ""
echo "2. 运行以下命令（需要输入一次密码）："
echo "   sudo sh -c \"echo '$SUDO_CONFIG' > /etc/sudoers.d/$USER\""
echo ""
echo "3. 验证配置："
echo "   sudo ls /  # 应该不再要求密码"
echo ""
echo "========================================="
echo "注意事项："
echo "- 这只需要配置一次"
echo "- 仅在WSL开发环境使用"
echo "- 生产环境请勿使用此配置"
echo "========================================="