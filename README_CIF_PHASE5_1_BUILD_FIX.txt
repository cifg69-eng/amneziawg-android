Cif VPN Phase 5.1 Build Fix
===========================

Что исправлено:

1. Убран импорт несуществующего класса:
   org.amnezia.awg.activity.LogViewerActivity

2. Кнопка "Диагностика" больше не открывает несуществующий Activity.
   Она показывает Snackbar, чтобы сборка не падала.

3. Фон cif_screen_background.xml сделан безопаснее:
   убраны отрицательные inset-значения в layer-list.

4. VPN-логика не изменена:
   центральная кнопка по-прежнему вызывает BaseFragment.setTunnelState(...).

Как применить:
1. Распаковать архив в корень проекта:
   C:\Users\kufik\Documents\GitHub\amneziawg-android

2. Согласиться на замену файлов.

3. GitHub Desktop:
   Summary: Cif VPN phase 5.1 build fix

4. Commit to master
5. Push origin
6. GitHub Actions → дождаться APK.

Если сборка снова упадёт:
- открыть красный build;
- раскрыть шаг "Build debug APK";
- прислать строку ошибки, где написано "error:" или "FAILURE:".
