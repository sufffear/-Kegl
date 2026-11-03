# Статус сборки и среды проекта Velaris (docs/BUILD_STATUS.md)

Дата обновления: 03.11.2026  
Текущий этап: **0B — Технический стенд фонового таймера, сигналов и прерываний**  
Статус этапа 0B: **Сборка `0.2.0-0b` (`versionCode 2`) и модульные/Robolectric тесты выполнены (`PASS`); ожидается аппаратная проверка на Samsung SM-S938B владельцем.**

---

## 1. Зафиксированные версии среды, SDK и целевого телефона

| Компонент | Фактическое значение (проверено) | Источник проверки |
|---|---|---|
| Стартовая среда | Google AI Studio (Build → Android, Linux 4.19.0-gvisor amd64) | Системное окружение контейнера |
| JDK / JVM | Eclipse Adoptium **21.0.13+9-LTS**, Java target `11` | `gradle --version`, `app/build.gradle.kts` |
| Gradle | **9.3.1** | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin (AGP) | **9.1.1** | `gradle/libs.versions.toml` |
| Kotlin | **2.2.10** | `gradle/libs.versions.toml` |
| Jetpack Compose BOM | **2024.09.00** (`activity-compose` 1.10.1, `lifecycle-*` 2.8.7) | `gradle/libs.versions.toml` |
| `compileSdk` / `targetSdk` / `minSdk` | **36** (`minorApiLevel = 1`) / **36** / **24** | `app/build.gradle.kts` |
| `applicationId` | **`com.velaris.app`** (неизменный) | `app/build.gradle.kts`, `Merged AndroidManifest.xml` |
| `versionName` / `versionCode` | **`0.2.0-0b`** / **`2`** (увеличен с `1` для обновления поверх 0A) | `app/build.gradle.kts` |
| Подпись сборки | `debugConfig` (`${rootDir}/debug.keystore`, алиас `androiddebugkey`) — идентична сборке 0A | `app/build.gradle.kts` |
| Целевой телефон владельца | **Samsung SM-S938B, Android 17 (API 37)** | Подтверждено владельцем при запуске сборки 0A (03.11.2026) |

---

## 2. Последний успешный APK этапа 0B и способ получения

- **Команда сборки и экспорта в папку релизов репозитория:**
  - `gradle :app:testDebugUnitTest :app:exportDebugApk` -> **`BUILD SUCCESSFUL`**
- **Готовый установочный файл APK в дереве проекта (вне `.gitignore`):**
  - **`releases/velaris-v0.2.0-0b-debug.apk`**
- **Совместимость обновления:**
  - Сохранены `applicationId = "com.velaris.app"` и тот же ключ подписи `debug.keystore`. Увеличен `versionCode = 2`. При установке на Samsung SM-S938B сборка `0.2.0-0b` обновит установленную версию `0.1.0-0a` поверх, без удаления приложения.

---

## 3. Выполненные проверки этапа 0B (с доказательствами)

1. **Компиляция и экспорт APK (`compile_applet`, `gradle :app:exportDebugApk`):**
   - Результат: **PASS**. Сформирован `releases/velaris-v0.2.0-0b-debug.apk`.
2. **Проверка итогового манифеста (`Merged AndroidManifest.xml`):**
   - Результат: **PASS**. `package="com.velaris.app"`, `android:versionCode="2"`, `android:versionName="0.2.0-0b"`, `android:allowBackup="false"`. Разрешение `android.permission.INTERNET` отсутствует. Объявлен `BenchTimerService` с `android:foregroundServiceType="specialUse"` и обязательным `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" ... />`.
3. **Автоматические тесты движка фаз и UI (`gradle :app:testDebugUnitTest`):**
   - Результат: **PASS** (8 тестов: защита от двойного старта, 3 сигнала подготовки без начисления активного времени, пауза 60 с без роста активного времени, ручное возобновление с остатка фазы, отмена диалога остановки «Завершить без зачёта?», защита от задержки планировщика ОС > 2500 мс, замена финального сигнала группы без двойного финала, проверка UI и манифеста в Robolectric).

---

## 4. Известные ограничения и следующий шаг

- **Честное ограничение обнаружения звонков без доступа к телефонной книге и `READ_PHONE_STATE`:**
  - По п. 7 ТЗ приложение не запрашивает разрешение на чтение состояния телефона и журнала звонков. Переход в паузу при звонке работает через системный `AudioFocus` (`OnAudioFocusChangeListener`). Если конкретный мессенджер при входящем звонке в беззвучном режиме не запрашивает аудиофокус у Android до снятия трубки, пауза сработает в момент захвата аудиофокуса звонком.
- **Следующий шаг:**
  - Владелец отправляет коммит в подключённый GitHub, скачивает `releases/velaris-v0.2.0-0b-debug.apk` на **Samsung SM-S938B (Android 17)**, устанавливает поверх версии 0A и выполняет аппаратные проверки стенда 0B (выключенный экран, пауза, отключение наушников, звонок). Этап 1 до команды владельца не начинается.
