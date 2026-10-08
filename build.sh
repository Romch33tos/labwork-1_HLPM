#!/usr/bin/env bash
set -e

SRC_DIR="src"
LIB_DIR="lib"
KOTLINX_CLI_JAR="${LIB_DIR}/kotlinx-cli-jvm-0.3.5.jar"
JAR_NAME="app.jar"
BUILD_DIR="build"

if [ ! -f "${KOTLINX_CLI_JAR}" ]; then
  echo "Не найден ${KOTLINX_CLI_JAR}. Скачайте kotlinx-cli-jvm-0.3.5.jar в каталог lib/"
  exit 1
fi

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}/classes"
rm -f "${JAR_NAME}"

kotlinc "${SRC_DIR}" \
  -cp "${KOTLINX_CLI_JAR}" \
  -include-runtime \
  -d "${BUILD_DIR}/app-with-runtime.jar"

cd "${BUILD_DIR}/classes"
jar xf "../app-with-runtime.jar"
cd - > /dev/null

cd "${BUILD_DIR}/classes"
jar xf "../../${KOTLINX_CLI_JAR}"
cd - > /dev/null

cat > "${BUILD_DIR}/MANIFEST.MF" <<EOF
Main-Class: MainKt
EOF

jar cfm "${JAR_NAME}" "${BUILD_DIR}/MANIFEST.MF" -C "${BUILD_DIR}/classes" .

rm -rf "${BUILD_DIR}"

echo "Сборка завершена: ${JAR_NAME}"
