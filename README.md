# FastFlow

Особистий офлайн-трекер інтервального голодування для Android.
Повне ТЗ — [`SPEC.md`](SPEC.md), конвенції розробки — [`CLAUDE.md`](CLAUDE.md).

## Збірка

```bash
./gradlew assembleDebug        # debug APK (підписаний debug-ключем, ставиться на телефон)
./gradlew installDebug         # зібрати і встановити на під'єднаний пристрій
./gradlew testDebugUnitTest    # юніт-тести доменної логіки
./gradlew assembleRelease      # release APK (потребує keystore.properties)
```

Готовий APK: `app/build/outputs/apk/debug/app-debug.apk`.

Кожен пуш у гілку розробки збирає APK у GitHub Actions і публікує його
в переднрелізі `dev-latest` — файл можна завантажити просто з телефона.

## Вимоги

- JDK 17
- Android SDK, compileSdk 35
- minSdk 26 (Android 8.0)

## Release-підпис

Якщо в корені лежить `keystore.properties` (у git не потрапляє), `assembleRelease`
підпише APK; без нього реліз збереться непідписаним:

```properties
storeFile=/абсолютний/шлях/fastflow.jks
storePassword=…
keyAlias=fastflow
keyPassword=…
```

## Структура

Пакети за SPEC §4: `data/`, `domain/`, `alarm/`, `ui/`, `di/`, `widget/`.
`domain/` не залежить від Android SDK — уся логіка статусів днів, стріків і
компенсації покрита юніт-тестами (SPEC §7).
