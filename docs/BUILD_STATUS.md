# Статус сборки и среды проекта Velaris (docs/BUILD_STATUS.md)

Дата обновления: 03.11.2026  
Текущий этап: **0A — Проверка среды, минимальной сборки и способа установки**  
Статус этапа 0A: **Сборка и локальные проверки в облачном контейнере выполнены (PASS); APK выгружен в отслеживаемую папку `releases/velaris-v0.1.0-0a-debug.apk` для отправки в подключённый GitHub.**

---

## 1. Зафиксированные версии среды и инструментов

| Компонент | Фактическое значение (проверено в среде) | Источник проверки |
|---|---|---|
| Стартовая среда | Google AI Studio (Build → Android, Linux 4.19.0-gvisor amd64) | Системное окружение контейнера |
| JDK / JVM | Eclipse Adoptium **21.0.13+9-LTS** (`/usr/lib/jvm/temurin-21-jdk-amd64`), Java target `11` | `gradle --version`, `app/build.gradle.kts` |
| Gradle | **9.3.1** | `gradle/wrapper/gradle-wrapper.properties`, `gradle --version` |
| Android Gradle Plugin (AGP) | **9.1.1** | `gradle/libs.versions.toml` |
| Kotlin | **2.2.10** | `gradle/libs.versions.toml` |
| KSP (Kotlin Symbol Processing) | **2.3.5** (доступен в каталоге, на этапе 0A не задействован) | `gradle/libs.versions.toml` |
| Jetpack Compose BOM | **2024.09.00** (`activity-compose` 1.10.1, `lifecycle-*` 2.8.7) | `gradle/libs.versions.toml` |
| `compileSdk` | **36** (`minorApiLevel = 1`) | `app/build.gradle.kts` |
| `targetSdk` | **36** | `app/build.gradle.kts` |
| `minSdk` | **24** (Android 7.0+) | `app/build.gradle.kts` |
| `applicationId` | **`com.velaris.app`** | `app/build.gradle.kts` и итоговый `Merged AndroidManifest.xml` |
| `versionName` / `versionCode` | **`0.1.0-0a`** / **`1`** | `app/build.gradle.kts` |
| Целевой телефон владельца | **Samsung (модель и версия Android / One UI определяются при первом запуске на телефоне и выводятся на карточке «Среда запуска»)** | Не угадывается заранее (по ТЗ п. 3); фиксируется после установки владельцем |

---

## 2. Последний успешный APK и способ получения через GitHub

- **Команда сборки и выгрузки в папку проекта:** `gradle :app:exportDebugApk` (`BUILD SUCCESSFUL`).
- **Готовый файл APK в дереве проекта (вне `.gitignore`):**
  - **`releases/velaris-v0.1.0-0a-debug.apk`**
  - При нажатии **Push to GitHub** в интерфейсе AI Studio этот файл попадает прямо в ваш репозиторий GitHub в папку `releases/`.
- **Дополнительный автоматический сборщик в GitHub Actions:**
  - Добавлен файл **`.github/workflows/build-apk.yml`** (собирает APK в облаке GitHub при каждом `push` и прикрепляет артефакт `velaris-v0.1.0-0a-debug-apk`).

---

## 3. Выполненные проверки этапа 0A (с доказательствами)

1. **Компиляция нативного проекта (`compile_applet` и `gradle :app:exportDebugApk`):**
   - Результат: **PASS** (`BUILD SUCCESSFUL`).
2. **Проверка итогового манифеста (`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`):**
   - Результат: **PASS**. Пакет `package="com.velaris.app"`, `android:versionCode="1"`, `android:versionName="0.1.0-0a"`, `android:allowBackup="false"`. Разрешение `android.permission.INTERNET` полностью отсутствует.
3. **Модульные и UI-тесты JVM / Robolectric (`gradle :app:testDebugUnitTest`):**
   - Результат: **PASS** (`BUILD SUCCESSFUL`). Проверены `AppBuildInfo` (domain), `BuildConfig` (`com.velaris.app`, `0.1.0-0a`), отсутствие `INTERNET` в манифесте и рендеринг Compose-экрана «Velaris · Техническая проверка» с номером версии.

---

## 4. Известные ограничения и условие перехода к этапу 0B

- **Что не проверено в контейнере:** реальная установка APK на физический телефон Samsung владельца.
- **Условие перехода к этапу 0B:** владелец отправляет изменения в подключённый GitHub (Push), скачивает `releases/velaris-v0.1.0-0a-debug.apk` из репозитория на Samsung, устанавливает его и подтверждает запуск экрана «Velaris · Техническая проверка».
