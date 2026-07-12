Cif VPN Phase 8.8 — Clean Product Pass

База:
- Phase 8.7 Full Visual Rebrand
- Phase 8.7.1 AppCompat Startup Fix

Исправлено:
1. Большая чёрная полоса:
   MainActivity теперь использует NoActionBar-тему и собственный Toolbar.
   На главном экране Toolbar имеет GONE и физически не занимает место.
   В деталях/редакторе Toolbar появляется, поэтому Back и меню не потеряны.

2. Обрезание снизу:
   удалена фиксированная высота 770dp;
   строка и CifVpnDashboardView используют wrap_content;
   высота вычисляется самим View;
   добавлен безопасный нижний запас для прокрутки.

3. Старые упоминания:
   окно импорта принудительно показывает только текст Cif VPN;
   для debug/release/googleplay добавлены прямые resource overlays;
   старое имя и URL больше не должны появляться в русской и английской версиях.

4. Настройки:
   старый логотип заменён знаком Cif;
   суффикс -debug скрыт только в пользовательском отображении;
   технический backend и его версия убраны с верхней карточки;
   переход на старый сайт удалён;
   заголовок — «Настройки Cif VPN».

5. Логотип:
   полный приложенный логотип сохранён для launcher и splash;
   в компактном UI используется прозрачный знак из этого же логотипа;
   квадрат с чёрным фоном убран из шапки и карточки сервера.

Не изменены:
- tunnel/
- GoBackend
- TunnelManager
- BaseFragment.setTunnelState
- ключи
- конфиги
- криптография
- package org.amnezia.awg
- нативный VPN engine

Установка:
1. Распаковать архив в корень репозитория с заменой файлов.
2. Commit: Cif VPN phase 8.8 clean product pass
3. Push origin.
4. GitHub Actions → :ui:assembleDebug
