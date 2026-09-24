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

success_count=0
total_count=0

total_count=$((total_count + 1))
run_case "успешное выполнение" 0 \
  --login alice --password qwerty --action read --resource A --volume 10

total_count=$((total_count + 1))
run_case "запрошена справка" 1 --help

total_count=$((total_count + 1))
run_case "неверный пароль" 2 \
  --login alice --password wrong --action read --resource A --volume 10

total_count=$((total_count + 1))
run_case "неверный логин" 3 \
  --login unknown --password qwerty --action read --resource A --volume 10

total_count=$((total_count + 1))
run_case "неизвестное действие" 4 \
  --login alice --password qwerty --action fly --resource A --volume 10

total_count=$((total_count + 1))
run_case "нет доступа" 5 \
  --login alice --password qwerty --action read --resource E --volume 5

total_count=$((total_count + 1))
run_case "несуществующий ресурс" 6 \
  --login alice --password qwerty --action read --resource A.Z --volume 5

total_count=$((total_count + 1))
run_case "некорректный формат ресурса" 7 \
  --login alice --password qwerty --action read --resource "A..B" --volume 5

total_count=$((total_count + 1))
run_case "превышение максимального объёма" 8 \
  --login alice --password qwerty --action read --resource A --volume 999

total_count=$((total_count + 1))
run_case "некорректный объём" 7 \
  --login alice --password qwerty --action read --resource A --volume 0
