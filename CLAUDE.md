# FastFlow — Android трекер інтервального голодування

Особистий застосунок для одного користувача. Без бекенду, акаунтів, аналітики та мережі.

## Джерело істини
Повне ТЗ — `SPEC.md`. Перед будь-якою задачею прочитай відповідні розділи. Якщо спека і код розходяться — спека права; якщо спека мовчить — запитай, не вигадуй.

## Стек (не змінювати без погодження)
- Kotlin, Jetpack Compose, Material 3, minSdk 26, targetSdk 35
- Hilt, Room, DataStore (Preferences), Navigation Compose (type-safe), Coroutines/Flow
- AlarmManager (`setExactAndAllowWhileIdle`) + BootReceiver для сповіщень; WorkManager лише для віджета
- Jetpack Glance для віджета
- Gradle KTS + version catalog, core library desugaring для `java.time`
- Жодних сторонніх бібліотек для графіків/кілець — тільки Canvas
- Дозвіл `INTERNET` НЕ додавати. `android:allowBackup="false"`

## Команди
- Збірка: `./gradlew assembleDebug`
- Встановити на телефон: `./gradlew installDebug`
- Тести: `./gradlew testDebugUnitTest`
- Реліз: `./gradlew assembleRelease` (keystore у `keystore.properties`, не комітити)

## Архітектурні правила
- Пакети: `data/`, `domain/`, `alarm/`, `ui/`, `di/` — див. SPEC §4
- `domain/` не імпортує `android.*`. Уся логіка статусів днів, стріків, компенсації — чисті функції з unit-тестами (SPEC §7 — обов'язкові кейси)
- Один `StateFlow<UiState>` на екран; UI без бізнес-логіки
- Після будь-якого запису fast'у в репозиторій → `AlarmScheduler.rescheduleAll()` (у UseCase)
- Кольори тільки з `AppPalette` через `CompositionLocal`. Захардкоджений `Color(0x…)` поза `ui/theme/` — помилка
- Рядки тільки в `res/values/strings.xml`, мова — українська, `resConfigs("uk")`
- Тема — лише темна (дві палітри Mint/System), `isSystemInDarkTheme()` не використовувати

## Процес
- Працюємо по фазах зі SPEC §6. Одна фаза — одна серія комітів. Не починати наступну фазу без явного «ок» після ручної перевірки
- Кожна фаза завершується робочим APK (`assembleDebug` без помилок, тести зелені)
- Коміти маленькі, повідомлення англійською в imperative: `feat(home): dual ring with overtime state`
- Перед кожним коммітом: `./gradlew testDebugUnitTest`
- Не додавати фічі, яких немає в спеці. Якщо здається, що чогось бракує — спитати

## Важливі пастки (SPEC §8)
- Samsung може вбивати receiver'и — onboarding з підказкою про батарею
- Alarm'и ідемпотентні: cancel усіх requestCode → plan заново
- Glance не має Canvas: кільце у віджеті малюється в Bitmap
- Статус дня може змінюватися заднім числом (компенсація) — перераховувати стрік після старту наступного fast'у
