#!/usr/bin/env bash

JAR_NAME="app.jar"
KOTLINX_CLI_JAR="lib/kotlinx-cli-jvm-0.3.5.jar"

if [ ! -f "${JAR_NAME}" ]; then
  echo "Сначала выполните ./build.sh"
  exit 1
fi
