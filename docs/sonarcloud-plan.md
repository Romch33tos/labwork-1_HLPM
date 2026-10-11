# План интеграции SonarCloud

## Предварительные требования

| Пункт | Требование |
|---|---|
| Java | JDK 11+ (SonarScanner CLI 7.2+ умеет подтягивать JDK автоматически) |
| Аккаунт SonarCloud | Регистрация через GitHub |
| Репозиторий GitHub | Код запушен в удалённый репозиторий |
| Сеть | Доступ к `sonarcloud.io` |

## Шаг 1. Аккаунт SonarCloud и импорт репозитория

1. Открыть [sonarcloud.io](https://sonarcloud.io).
2. Войти через **GitHub**.
3. Разрешить SonarCloud доступ к организации/репозиториям.
4. **«+»** → **«Analyze new project»**.
5. Выбрать репозиторий → **«Set Up»**.

После импорта записать:

- **Organization Key** — например, `your-org`;
- **Project Key** — например, `your-org_your-repo`.

## Шаг 2. Токен аутентификации

1. SonarCloud → аватар → **«My Account»**.
2. Вкладка **«Security»**.
3. Ввести имя токена (например, `github-actions-token`).
4. **«Generate»**.
5. Скопировать токен сразу — показывается один раз.

Добавить токен в GitHub Secrets:

- Репозиторий → **Settings** → **Secrets and variables** → **Actions**.
- **«New repository secret»**.
- Name: `SONAR_TOKEN`.
- Value: сохранённый токен.

## Шаг 3. Файл `sonar-project.properties`

Создать в корне проекта:

```properties
# Идентификаторы проекта (получить после создания проекта в SonarCloud)
sonar.projectKey=your-org_your-repo
sonar.organization=your-org

# Метаданные (опционально)
sonar.projectName=Access Control CLI
sonar.projectVersion=1.0

# Исходники относительно этого файла
sonar.sources=src

# Исключения
sonar.exclusions=**/build/**,**/lib/**,**/*.jar

# Кодировка
sonar.sourceEncoding=UTF-8

# Отчёт Detekt (удалить, если Detekt не используется)
sonar.kotlin.detekt.reportPaths=build/reports/detekt.xml
```

## Шаг 4. Установка SonarScanner CLI

### Вариант A. Прямая загрузка (для локального запуска)

```bash
wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-7.2.0.1092-linux.zip
unzip sonar-scanner-cli-7.2.0.1092-linux.zip
export PATH=$PATH:$(pwd)/sonar-scanner-7.2.0.1092-linux/bin
```

Проверка:

```bash
sonar-scanner -h
```

### Вариант B. Docker

```bash
docker run --rm \
  -e SONAR_HOST_URL="https://sonarcloud.io" \
  -e SONAR_TOKEN="your-token" \
  -v "$(pwd):/usr/src" \
  sonarsource/sonar-scanner-cli
```

## Шаг 5. Локальный запуск анализа

```bash
export SONAR_TOKEN="your-token-here"

sonar-scanner \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=$SONAR_TOKEN
```

При успехе в терминале появится `ANALYSIS SUCCESSFUL`.

## Шаг 6. Автоматизация через GitHub Actions

Файл `.github/workflows/sonarcloud.yml`:

```yaml
name: SonarCloud Analysis

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
    types: [opened, synchronize, reopened]

jobs:
  sonarcloud:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Build project
        run: |
          chmod +x build.sh
          ./build.sh

      - name: SonarCloud Scan
        uses: SonarSource/sonarqube-scan-action@v5
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

      - name: Check Quality Gate
        uses: SonarSource/sonarqube-quality-gate-action@v1
        timeout-minutes: 5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

Что важно в этом workflow:

- `fetch-depth: 0` — SonarCloud нужна полная история для инкрементального анализа.
- `build.sh` выполняется до сканирования, чтобы появились артефакты.
- `SONAR_TOKEN` читается из GitHub Secrets (шаг 2).
- Проверка Quality Gate блокирует мерж PR, если качество ниже порога.

## Шаг 7. Quality Gate

По умолчанию SonarCloud использует **Sonar way**:

| Метрика | Порог по умолчанию |
|---|---|
| Покрытие нового кода | ≥ 80% |
| Дублирование нового кода | ≤ 3% |
| Критические проблемы в новом коде | 0 |
| Проверка security hotspots | 100% |
| Рейтинг поддерживаемости нового кода | A |

Кастомный Quality Gate:

1. Страница проекта → **Administration** → **Quality Gate**.
2. **«Create»** — создать свой порог.
3. Задать значения и привязать к проекту.

## Шаг 8. Проверка интеграции

1. Запушить код, чтобы сработал workflow:

```bash
git add .
git commit -m "Add SonarCloud integration"
git push origin lab3-analysis
```

2. Проверить статус GitHub Actions:
   - репозиторий → вкладка **Actions**;
   - должен появиться workflow `SonarCloud Analysis`.

3. Проверить результаты в SonarCloud:
   - открыть [sonarcloud.io](https://sonarcloud.io);
   - проект → Issues, Coverage, Duplications.

4. Статус Quality Gate в PR:
   - на странице PR появится ✅ или ❌.
