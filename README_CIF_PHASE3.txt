Cif VPN Phase 3 — clean tunnel screen

Что исправлено:
- Полностью убран большой информационный блок.
- Убраны тексты про “не фейковую кнопку”, “real core” и прочий мусор.
- Главный экран теперь показывает только список туннелей и реальный переключатель.
- Логика VPN не меняется: переключатель остаётся штатным и вызывает fragment::setTunnelState.

Как применить:
1. Распаковать архив в корень проекта:
   C:\Users\kufik\Documents\GitHub\amneziawg-android
2. Согласиться на замену файлов.
3. GitHub Desktop: Commit → Push origin.
4. GitHub Actions соберёт новый APK.
