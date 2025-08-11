# WSL密码重置和免密码配置指南

## 步骤1：在Windows PowerShell中重置密码（以管理员身份运行）

打开Windows PowerShell（管理员），执行：

```powershell
# 查看WSL分发版名称
wsl -l -v

# 设置默认用户为root（假设你的分发版是Ubuntu）
ubuntu config --default-user root

# 如果是其他版本，使用对应命令：
# ubuntu2404 config --default-user root
# debian config --default-user root
```

## 步骤2：进入WSL，重置用户密码

在PowerShell中输入 `wsl` 进入Linux（此时是root用户）：

```bash
# 你现在是root用户，不需要密码
# 为sheen用户设置简单密码（比如: 123）
passwd sheen
# 输入新密码: 123
# 再次输入: 123
```

## 步骤3：配置sudo免密码

还在root用户下，执行：

```bash
# 为sheen用户配置sudo免密码
echo 'sheen ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/sheen

# 设置正确权限
chmod 440 /etc/sudoers.d/sheen

# 退出WSL
exit
```

## 步骤4：恢复默认用户

回到Windows PowerShell：

```powershell
# 将默认用户改回sheen
ubuntu config --default-user sheen

# 如果是其他版本：
# ubuntu2404 config --default-user sheen
```

## 步骤5：验证配置

重新打开WSL终端：

```bash
# 测试sudo（不应该要求密码）
sudo ls /
sudo apt update
```

---

## 备选方法：如果上述方法不行

### 在Windows中直接编辑WSL文件

1. 在Windows资源管理器中访问：
   `\\wsl$\Ubuntu\etc\sudoers.d\`

2. 用记事本创建文件 `sheen`，内容：
   ```
   sheen ALL=(ALL) NOPASSWD:ALL
   ```

3. 保存文件

4. 重启WSL：
   ```powershell
   wsl --shutdown
   wsl
   ```

---

## 完成后的效果

- 用户sheen可以使用sudo而不需要输入密码
- 所有sudo命令都会直接执行
- 仅在开发环境使用，生产环境请勿这样配置