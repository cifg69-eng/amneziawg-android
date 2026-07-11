# Cif VPN Phase 7 — Real 3D Power + Real App Bypass

## Что изменено

### 1. Центральная кнопка

Добавлен настоящий Android Custom View:

`ui/src/main/java/org/amnezia/awg/widget/CifVpnPowerView.kt`

Он рисует интерфейс через Canvas:
- многослойный объёмный круг;
- свет и глубину;
- собственную power-иконку без шрифтового символа;
- SweepGradient-бегунок по окружности;
- быструю анимацию CONNECTING;
- спокойную анимацию ON;
- состояние OFF без дешёвого неона;
- press microinteraction.

Внешние изображения и Lottie не нужны.

Кнопка вызывает существующий `BaseFragment.setTunnelState(...)`. VPN engine не заменён.

### 2. Реальный белый список приложений

Добавлен:

`ui/src/main/java/org/amnezia/awg/util/SmartBypassManager.kt`

Он:
- находит установленные популярные банки, Госуслуги, MAX/VK, маркетплейсы, Яндекс и операторов;
- показывает пользователю найденный список;
- после подтверждения сохраняет package names в `ExcludedApplications` текущего туннеля;
- существующий GoBackend передаёт эти package names в Android `VpnService.Builder.addDisallowedApplication(...)`;
- если туннель включён, текущий `TunnelManager.setTunnelConfig(...)` применяет обновлённую конфигурацию к работающему туннелю.

Есть ручной выбор приложений через уже существующий `AppListDialogFragment`, без перехода в экран приватных ключей и полного конфига.

## Важное ограничение по сайтам

В этой версии не заявлен фейковый domain bypass.

Текущий Android backend надёжно поддерживает app-level exclusion. Идеальное исключение доменов требует отдельного DNS/routing слоя, потому что Android VPN маршрутизирует IP, а адреса CDN динамически меняются. Простой TXT или копирование доменов не является готовой функцией.

## Изменённые файлы

- `ui/src/main/java/org/amnezia/awg/widget/CifVpnPowerView.kt` — новый
- `ui/src/main/java/org/amnezia/awg/util/SmartBypassManager.kt` — новый
- `ui/src/main/java/org/amnezia/awg/fragment/TunnelListFragment.kt`
- `ui/src/main/res/layout/tunnel_list_item.xml`
- `ui/src/main/res/drawable/cif_status_chip_bg.xml` — новый
- `ui/src/main/res/values/cif_strings.xml`

## Применение

Распаковать содержимое архива в корень:

`C:\Users\kufik\Documents\GitHub\amneziawg-android`

Согласиться на замену файлов.

GitHub Desktop:
1. Summary: `Cif VPN phase 7 real 3D and smart bypass`
2. Commit to master
3. Push origin
4. GitHub Actions → дождаться debug APK

## Проверка

### OFF
- тёмная объёмная кнопка;
- бегунок не вращается;
- нажатие вызывает Android VPN permission при первом запуске.

### CONNECTING
- световой сегмент быстро движется по окружности;
- интерфейс показывает «Подключение».

### ON
- мягкое бирюзовое свечение;
- бегунок медленно движется по кольцу;
- повторное нажатие отключает реальный туннель.

### Белый список
1. Нажать «Умный белый список».
2. Проверить список найденных установленных приложений.
3. Нажать «Применить».
4. Перезапустить нужное банковское приложение.
5. В ручном выборе убедиться, что оно отмечено в режиме исключения.
6. Проверить, что приложение выходит напрямую, а браузер/ChatGPT продолжает работать через VPN.

## Предварительная проверка пакета

- XML-файлы проверены на корректность структуры.
- В коде нет ссылок на несуществующую `LogViewerActivity`.
- Кнопка не использует скрытый ToggleSwitch.
- Белый список не копирует TXT и не открывает приватный конфиг.
- Новые классы используют только Android SDK и уже существующие классы проекта.
