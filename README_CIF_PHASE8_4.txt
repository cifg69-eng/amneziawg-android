Cif VPN Phase 8.4 — Render Layer Fix

ТОЧНАЯ ПРИЧИНА ИЗ RUNTIME-ЛОГА:
CifVpnDashboardView принудительно переводился в LAYER_TYPE_SOFTWARE.
На устройстве для слоя требовалось 11 145 600 байт, а Android разрешал
10 368 000 байт. Android поэтому не рисовал View вообще и оставлял синий фон.

ИСПРАВЛЕНИЕ:
Удалён только вызов:
setLayerType(LAYER_TYPE_SOFTWARE, null)

Теперь экран рисуется штатным аппаратно-ускоренным Canvas окна.
VPN engine, конфиги, ключи, RecyclerView, белый список и setTunnelState не изменены.

ПРИМЕНЕНИЕ:
1. Распаковать архив в корень репозитория с заменой файла.
2. Commit: Cif VPN phase 8.4 render layer fix
3. Push origin.
4. Собрать APK в GitHub Actions.
