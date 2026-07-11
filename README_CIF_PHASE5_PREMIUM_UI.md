# Cif VPN — Phase 5 Premium Working UI

Пакет меняет только главный UI-экран и не переписывает VPN engine.

Главное: центральная кнопка вызывает реальную штатную функцию `setTunnelState(binding.root, item.state != Tunnel.State.UP)`, а не имитирует подключение.

Изменённые зоны:
- главный список туннелей;
- карточка туннеля;
- центральная 3D-кнопка;
- анимации OFF / CONNECTING / ON;
- доменный whitelist для split tunneling.

Применение:
1. Распаковать в корень проекта.
2. Заменить файлы.
3. Commit: `Cif VPN phase 5 premium working UI`.
4. Push origin.
5. GitHub Actions → дождаться APK.

Белые списки:
- `amnezia_bypass_domains_ru.txt` — ручной импорт в Amnezia, режим: адреса из списка не должны открываться через VPN.
- `ui/src/main/assets/cif_bypass_domains_ru.txt` — копия внутри проекта.
- `ui/src/main/assets/cif_bypass_apps_ru_packages.txt` — список популярных package name для будущего app-level exclusion. Автоматически выключать VPN для приложений без подтверждения пользователя нельзя.
