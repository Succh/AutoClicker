#!/bin/bash
# AutoClicker GitHub 推送脚本
# 在 Termux 中运行: bash /sdcard/Documents/AutoClicker/setup_github.sh

echo "========================================="
echo "  AutoClicker GitHub 自动推送脚本"
echo "========================================="

# 1. 检查 git
if ! command -v git &> /dev/null; then
    echo "❌ 未找到 git，正在安装..."
    pkg install git openssh -y
fi

# 2. 检查存储权限
if [ ! -d "/sdcard/Documents/AutoClicker" ]; then
    echo "⚠️  无法访问 /sdcard/Documents"
    echo "请先运行: termux-setup-storage"
    echo "然后授予存储权限，再重新运行此脚本"
    exit 1
fi

# 3. 配置 Git（如果未配置）
if [ -z "$(git config --global user.name)" ]; then
    echo ""
    echo "📝 请输入你的 GitHub 用户名:"
    read -r GIT_USER
    echo "📝 请输入你的 GitHub 邮箱:"
    read -r GIT_EMAIL
    git config --global user.name "$GIT_USER"
    git config --global user.email "$GIT_EMAIL"
    echo "✅ Git 已配置: $GIT_USER <$GIT_EMAIL>"
fi

# 4. 进入项目目录
cd /sdcard/Documents/AutoClicker || exit

# 5. 初始化仓库（如果还没有）
if [ ! -d ".git" ]; then
    git init
    git branch -M main
    echo "✅ Git 仓库已初始化"
fi

# 6. 添加远程仓库
REMOTE_URL=$(git remote get-url origin 2>/dev/null)
if [ -z "$REMOTE_URL" ]; then
    echo ""
    echo "📝 请输入你的 GitHub 仓库地址:"
    echo "   格式: https://github.com/用户名/AutoClicker.git"
    read -r REPO_URL
    git remote add origin "$REPO_URL"
    echo "✅ 远程仓库已添加"
else
    echo "✅ 远程仓库已存在: $REMOTE_URL"
fi

# 7. 添加所有文件并提交
echo ""
echo "📦 正在打包提交..."
git add .
git commit -m "init: AutoClicker 自动化点击工具

- 无障碍点击服务 (AccessibilityClickService)
- HTTP API 服务器 (HttpServerService)  
- 自动化执行引擎 (AutomationEngine)
- GitHub Actions 自动编译"

# 8. 推送
echo ""
echo "🚀 正在推送到 GitHub..."
git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "✅ 推送成功！"
    echo ""
    echo "GitHub Actions 正在自动编译..."
    echo "编译完成后，在这里下载 APK:"
    echo "https://github.com/用户名/AutoClicker/actions"
    echo "========================================="
else
    echo ""
    echo "❌ 推送失败，可能需要配置 GitHub 认证"
    echo "方案A: 使用 GitHub Personal Access Token"
    echo "  1. 打开 https://github.com/settings/tokens"
    echo "  2. 生成 token，勾选 repo 权限"
    echo "  3. 推送时用户名输入 token，密码留空"
    echo ""
    echo "方案B: 使用 SSH"
    echo "  运行: ssh-keygen -t ed25519"
    echo "  然后把公钥添加到 GitHub"
fi
