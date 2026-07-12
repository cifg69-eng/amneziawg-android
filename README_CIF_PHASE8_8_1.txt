Cif VPN Phase 8.8.1 — Duplicate app_name Fix

Причина сборки:
в одном и том же source set ui/src/debug/res/values ресурс app_name
был объявлен одновременно в strings.xml и cif_rebrand_overrides.xml.

Исправление:
- app_name остаётся только в ui/src/debug/res/values/strings.xml;
- его значение заменено на Cif VPN;
- из cif_rebrand_overrides.xml дубликат удалён;
- остальные строки ребрендинга сохранены.

VPN engine и логика подключения не изменялись.
