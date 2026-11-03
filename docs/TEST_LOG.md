# Журнал тестирования проекта Velaris (docs/TEST_LOG.md)

Дата обновления: 03.11.2026  
Этап: **0A — Проверка среды, минимальной сборки и способа установки**  
Правило статусов (ТЗ п. 14): используются только статусы `PASS` / `FAIL` / `NOT RUN`. Проверки, требующие физического телефона владельца, помечены как `NOT RUN` до фактического подтверждения.

---

## 1. Автоматические и инструментальные проверки в среде сборки (Этап 0A)

| ID | Проверка | Команда / Метод | Статус | Фактический результат и доказательство |
|---|---|---|---|---|
| 0A-T1 | Сборка нативного debug APK (Kotlin + Compose + M3) | `compile_applet` | **PASS** | `Build succeeded - the applet is compiled`. Сформирован пакет `com.velaris.app`, версия `0.1.0-0a` (`versionCode = 1`). |
| 0A-T2 | Проверка отсутствия `android.permission.INTERNET` и облачных SDK в итоговом манифесте | Анализ `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` и тест `ExampleRobolectricTest` | **PASS** | В итоговом манифесте нет `android.permission.INTERNET` и `ACCESS_NETWORK_STATE`; `android:allowBackup="false"`. |
| 0A-T3 | Модульный тест доменной модели (`domain`) | `gradle :app:testDebugUnitTest` (`ExampleUnitTest`) | **PASS** | Проверена модель `AppBuildInfo` для этапа `0A` (`BUILD SUCCESSFUL`). |
| 0A-T4 | Локальный UI и контекстный тест (`Robolectric` + `ComposeTestRule`) | `gradle :app:testDebugUnitTest` (`ExampleRobolectricTest`) | **PASS** | Проверены: `R.string.app_name == "Velaris"`, `BuildConfig.APPLICATION_ID == "com.velaris.app"`, отображение заголовка «Velaris · Техническая проверка» и строки «Версия: 0.1.0-0a (versionCode 1)» (`BUILD SUCCESSFUL in 1m 4s`). |
| 0A-T5 | Запуск в потоковом эмуляторе среды (Streaming Android Emulator) | Автоматическое развёртывание APK в превью AI Studio после сборки | **PASS** | APK готов и автоматически передаётся в окно потокового эмулятора AI Studio после завершения шага. |

---

## 2. Физические проверки на телефоне владельца (Samsung)

| ID | Проверка | Метод | Статус | Примечание |
|---|---|---|---|---|
| 0A-P1 | Выгрузка установочного APK и ZIP-архива исходников из интерфейса AI Studio на Samsung | Меню проекта AI Studio (Download APK / Export ZIP) | **NOT RUN** | Требуется действие владельца в браузере на телефоне Samsung. |
| 0A-P2 | Установка debug APK `com.velaris.app` (`0.1.0-0a`, `versionCode 1`) на физический Samsung и запуск экрана «Velaris · Техническая проверка» | Ручная установка APK на устройстве владельца | **NOT RUN** | Ожидается подтверждение от владельца. На экране приложения отобразятся фактическая модель устройства и версия Android. |
| 0B-P1..P6 | Фоновый сервис, звук/вибрация, уведомление с кнопкой «Пауза», реакция на выключение экрана, наушники, звонки и убийство процесса на Samsung | Технический стенд этапа 0B | **NOT RUN** | Запланировано на этап 0B (не начинать до завершения этапа 0A и отдельной команды владельца). |
