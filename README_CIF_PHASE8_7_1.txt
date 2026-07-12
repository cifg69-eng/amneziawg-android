Cif VPN Phase 8.7.1 — AppCompat Startup Fix

Точная причина подтверждена runtime-логом:
values-v31/cif_splash.xml переопределял CifMainTheme без parent="AppTheme".
На Android 12+ MainActivity запускалась с темой, которая не наследуется от
Theme.AppCompat, поэтому AppCompatActivity завершалась сразу при старте.

Исправление:
- CifMainTheme в values-v31 теперь наследуется от AppTheme;
- сохранён windowActionBarOverlay=true, поэтому исправление чёрной полосы не потеряно;
- сохранены фирменный splash screen, логотип и тёмные системные панели;
- VPN engine, конфиги, ключи и логика подключения не изменялись.

Применение:
1. Распаковать архив в корень репозитория с заменой файла.
2. Commit: Cif VPN phase 8.7.1 AppCompat startup fix
3. Push origin.
4. Собрать APK через GitHub Actions.
