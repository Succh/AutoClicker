#!/bin/bash
# UniFeed Android 构建环境一键重建脚本
# 用于 MonkeyCode 云环境被回收后快速重建 JDK + Gradle + Android SDK
# 用法: bash setup-android-env.sh
# 幂等设计: 已安装的组件会跳过，可重复运行

set -e

JAVA_DIR="/usr/local/java"
GRADLE_DIR="/opt/gradle"
SDK_DIR="/root/android-sdk"

echo "=== UniFeed Android 环境重建 ==="
echo "目标: JDK 21 -> $JAVA_DIR, Gradle 9.4.1 -> $GRADLE_DIR, Android SDK -> $SDK_DIR"
echo ""

# 1. JDK 21
if [ -x "$JAVA_DIR/bin/java" ]; then
    echo "[1/3] JDK 已存在，跳过: $($JAVA_DIR/bin/java -version 2>&1 | head -1)"
else
    echo "[1/3] 安装 JDK 21..."
    mkdir -p "$JAVA_DIR"
    cd /tmp
    # Temurin JDK 21 (Adoptium)
    JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_linux_hotspot_21.0.5_11.tar.gz"
    curl -sSL -o jdk21.tar.gz "$JDK_URL"
    tar xzf jdk21.tar.gz -C "$JAVA_DIR" --strip-components=1
    rm jdk21.tar.gz
    echo "  JDK 安装完成: $($JAVA_DIR/bin/java -version 2>&1 | head -1)"
fi

export JAVA_HOME="$JAVA_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

# 2. Gradle 9.4.1
if [ -x "$GRADLE_DIR/bin/gradle" ]; then
    echo "[2/3] Gradle 已存在，跳过: $($GRADLE_DIR/bin/gradle --version 2>/dev/null | grep Gradle | head -1)"
else
    echo "[2/3] 安装 Gradle 9.4.1..."
    mkdir -p "$GRADLE_DIR"
    cd /tmp
    GRADLE_URL="https://services.gradle.org/distributions/gradle-9.4.1-bin.zip"
    curl -sSL -o gradle.zip "$GRADLE_URL"
    unzip -q gradle.zip -d /tmp
    cp -r /tmp/gradle-9.4.1/* "$GRADLE_DIR/"
    rm -rf /tmp/gradle-9.4.1 gradle.zip
    echo "  Gradle 安装完成"
fi

# 3. Android SDK
if [ -d "$SDK_DIR/cmdline-tools/latest" ]; then
    echo "[3/3] Android SDK 已存在，跳过: $SDK_DIR"
else
    echo "[3/3] 安装 Android SDK..."
    mkdir -p "$SDK_DIR/cmdline-tools"
    cd /tmp
    # Android command line tools (linux)
    CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    curl -sSL -o cmdtools.zip "$CMDLINE_URL"
    unzip -q cmdtools.zip -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm cmdtools.zip

    export ANDROID_HOME="$SDK_DIR"
    export ANDROID_SDK_ROOT="$SDK_DIR"
    export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

    echo "  接受 SDK 许可并安装必要包..."
    yes | "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1 || true
    "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" \
        "platform-tools" \
        "platforms;android-34" \
        "build-tools;34.0.0" > /dev/null 2>&1
    echo "  Android SDK 安装完成"
fi

export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"

echo ""
echo "=== 环境就绪 ==="
echo "JAVA_HOME=$JAVA_DIR"
echo "ANDROID_HOME=$SDK_DIR"
echo "Gradle: $GRADLE_DIR/bin/gradle"
echo ""
echo "构建命令示例:"
echo "  export JAVA_HOME=$JAVA_DIR ANDROID_HOME=$SDK_DIR"
echo "  $GRADLE_DIR/bin/gradle assembleDebug"
