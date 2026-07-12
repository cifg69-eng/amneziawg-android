Cif VPN Phase 8.8.2 — Kotlin Compile Safety Fix

Исправлены статически некорректные вызовы в SettingsActivity:
- findPreference() больше не передаётся в removePreference() как nullable-значение;
- для каждого Preference используется локальная переменная с явным типом;
- устранены места, где Kotlin не мог надёжно вывести generic-тип;
- список технических настроек собирается через listOfNotNull.

Из VersionPreference удалено необязательное свойство isCopyingEnabled,
чтобы не зависеть от версии AndroidX Preference.

VPN engine, конфиги, ключи, импорт и подключение не изменены.
