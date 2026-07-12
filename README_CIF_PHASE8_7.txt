Cif VPN Phase 8.7 — Full Visual Rebrand

База:
Cif VPN Phase 8.6 — Top Gap + Factual Server.

Что сделано:
- использован именно приложенный пользователем логотип, без перерисовки;
- заменены launcher icon, round icon и adaptive icon;
- добавлен monochrome icon для Android 13+;
- заменена иконка Quick Settings;
- заменён Android TV banner;
- добавлен Cif splash screen для старых и новых Android;
- логотип встроен в шапку главного экрана;
- логотип встроен в карточку фактического VPN-сервера;
- логотип встроен в пустой экран до импорта;
- новый брендовый цвет по умолчанию — розовый;
- сохранено изменение цвета внутри приложения;
- сохранён фактический Endpoint сервера;
- сохранено исправление Phase 8.6 против верхней чёрной полосы;
- все видимые строковые упоминания Amnezia/AmneziaWG заменяются на Cif VPN
  во всех локалях одним безопасным скриптом.

Неприкосновенная граница:
- модуль tunnel не изменён;
- VPN engine не изменён;
- org.amnezia.awg не переименовывается;
- native libraries не переименовываются;
- ключи, конфиги, формат .conf и setTunnelState не изменены;
- внутренние технические идентификаторы остаются для совместимости;
- лицензии и copyright notices в исходном коде не удаляются.

Применение:
1. Распаковать архив в:
   C:\Users\kufik\Documents\GitHub\amneziawg-android
   с заменой файлов.
2. Один раз запустить:
   APPLY_CIF_VISUAL_REBRAND.cmd
3. GitHub Desktop:
   Commit: Cif VPN phase 8.7 full visual rebrand
4. Push origin.
5. GitHub Actions:
   ./gradlew :ui:assembleDebug

Почему есть отдельный CMD:
Он автоматически очищает видимые строки во всех языковых каталогах values-*,
не затрагивая package names, Java/Kotlin-классы и VPN engine.
