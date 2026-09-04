# Стартовий промпт для Claude Code (Opus) — Фаза 0

Скопіювати в Claude Code після `/model opus` у порожньому репо, де вже лежать `SPEC.md` і `CLAUDE.md`.

---

Прочитай `CLAUDE.md` і `SPEC.md` повністю. Ми будуємо цей застосунок строго по фазах зі SPEC §6.

Зараз — **Фаза 0 (каркас)**. Зроби:

1. Ініціалізуй Android-проєкт `FastFlow` (`com.oleksandr.fastflow`): Gradle KTS, version catalog, Compose BOM, Material 3, Hilt, Room, DataStore, Navigation Compose, WorkManager, Glance, desugaring. minSdk 26, targetSdk 35. `allowBackup="false"`, без `INTERNET`.
2. Структуру пакетів зі SPEC §4 (порожні пакети з `package-info` або першими класами).
3. `ui/theme/`: `AppPalette` (data class з усіма токенами зі SPEC §5.1, дві реалізації — `MintPalette`, `SystemPalette`), `LocalAppPalette`, `FastFlowTheme` (тільки темна, підключає Inter і `tnum`), типографіка зі SPEC §5.1.
4. Нижню навігацію з 4 табами (Таймер / Історія / Статистика / Налаштування) з порожніми екранами-заглушками, стилізовану під iOS tab bar (SPEC §5.1).
5. `strings.xml` українською для всього, що вже є; `resConfigs("uk")`.
6. `README.md` з командами збірки.

Обмеження: не реалізовуй нічого з Фаз 1+. Не додавай бібліотек поза списком у CLAUDE.md. Кольори лише через `AppPalette`.

Коли `./gradlew assembleDebug` проходить — зупинись, покажи дерево проєкту і список того, що потрібно перевірити руками на телефоні. Наступну фазу почнемо після мого «ок».
