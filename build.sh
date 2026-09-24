#!/usr/bin/env bash
set -e

SRC_DIR="src"
LIB_DIR="lib"
KOTLINX_CLI_JAR="${LIB_DIR}/kotlinx-cli-jvm-0.3.5.jar"
JAR_NAME="app.jar"

if [ ! -f "${KOTLINX_CLI_JAR}" ]; then
  echo "Не найден ${KOTLINX_CLI_JAR}. Скачайте kotlinx-cli-jvm-0.3.5.jar в каталог lib/"
  exit 1
fi
