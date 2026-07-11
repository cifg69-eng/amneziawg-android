# Cif VPN Phase 6 Clean Premium

Что сделано:
- убраны псевдо-3D кольца, квадраты, радужные свечения и технические надписи;
- главный экран стал минималистичным: статус, профиль, большая чистая кнопка, белый список, импорт;
- кнопка подключения не фейковая: она вызывает существующий `setTunnelState(binding.root, item.state != Tunnel.State.UP)`;
- VPN engine, VpnService, импорт конфигов и backend не менялись;
- белый список теперь не открывает конфиг: он показывает диалог и копирует готовый список доменов для режима Amnezia “Адреса из списка не должны открываться через VPN”.

Изменённые файлы:
- ui/src/main/res/layout/tunnel_list_fragment.xml
- ui/src/main/res/layout/tunnel_list_item.xml
- ui/src/main/java/org/amnezia/awg/fragment/TunnelListFragment.kt
- ui/src/main/res/values/cif_strings.xml
- ui/src/main/res/values/cif_colors.xml
- ui/src/main/res/drawable/cif_clean_*.xml
- ui/src/main/res/anim/cif_clean_connecting_pulse.xml
- ui/src/main/assets/cif_bypass_domains_ru.txt
- amnezia_bypass_domains_ru.txt

Как применить:
1. Распаковать в корень проекта.
2. Commit: Cif VPN phase 6 clean premium UI
3. Push origin.
4. GitHub Actions → дождаться APK.

Проверка:
- OFF: статус отключено, кнопка OFF.
- CONNECTING: статус подключение, мягкая пульсация кнопки.
- ON: статус защищено, кнопка ON.
- Белый список: открывается диалог, список копируется в буфер.
- Импорт: открывает штатный bottom sheet добавления конфигурации.
