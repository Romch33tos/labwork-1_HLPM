#!/usr/bin/env bash

JAR_NAME="app.jar"
KOTLINX_CLI_JAR="lib/kotlinx-cli-jvm-0.3.5.jar"

run_case() {
  local description="$1"
  local expected_code="$2"
  shift 2

  java -cp "${JAR_NAME};${KOTLINX_CLI_JAR}" MainKt "$@" > /dev/null 2>&1
  local actual_code=$?

  if [ "${actual_code}" -eq "${expected_code}" ]; then
    echo "OK   [${description}] ожидался ${expected_code}, получен ${actual_code}"
    success_count=$((success_count + 1))
  else
    echo "FAIL [${description}] ожидался ${expected_code}, получен ${actual_code}"
  fi
}

if [ ! -f "${JAR_NAME}" ]; then
  echo "Сначала выполните ./build.sh"
  exit 1
fi
