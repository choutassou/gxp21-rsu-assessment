#!/bin/bash

# アプリケーションのホームディレクトリを設定
APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Javaの実行パスを設定（必要に応じて変更）
JAVA_EXE="java"

# JVMオプション
JAVA_OPTS="-Xms512m -Xmx2g -Dfile.encoding=UTF-8"

# ログ設定
LOG_CONFIG="-Dlogback.configurationFile=${APP_HOME}/config/logback.xml"

# 設定ファイルパス
CONFIG_FILE="${APP_HOME}/config/Assessment.properties"

# 作業ディレクトリを設定ファイルのある場所に変更
cd "${APP_HOME}/config"

echo "Starting GXP21 Amplification Data Application..."
echo "Config file: ${CONFIG_FILE}"
echo "Working directory: $(pwd)"

# アプリケーション実行
"${JAVA_EXE}" ${JAVA_OPTS} ${LOG_CONFIG} -jar "${APP_HOME}/lib/gxp21-amplification-data-1.0.0.jar"