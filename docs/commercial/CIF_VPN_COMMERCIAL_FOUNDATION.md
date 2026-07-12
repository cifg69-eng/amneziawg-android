# Cif VPN v2.0 Commercial Foundation

## Цель

Превратить существующий Android-клиент Cif VPN в коммерческий сервис, сохранив
рабочий AmneziaWG engine без изменений. Коммерческий слой отвечает за аккаунты,
устройства, подписки, выбор узлов и автоматическую выдачу конфигураций.

## Зафиксированное состояние Android-клиента

- Модули `:ui` и `:tunnel`; minSdk 24, target/compile SDK 35, Java 17.
- Реальный tunnel lifecycle проходит через `BaseFragment.setTunnelState`,
  `TunnelManager` и существующий `Backend`.
- Импорт файлов и QR выполняется существующим `TunnelImporter`.
- App-level split tunneling хранится в `IncludedApplications` или
  `ExcludedApplications`. Userspace backend применяет исключения через
  `VpnService.Builder.addDisallowedApplication`.
- Главный экран Cif VPN реализован поверх `TunnelListFragment` и кастомного
  `CifVpnDashboardView`.
- Сборка APK выполняется GitHub Actions на Linux с recursive submodules.

## Неприкосновенная граница

Commercial Foundation не меняет:

- модуль `:tunnel`;
- AmneziaWG cryptography и native libraries;
- формат существующего `.conf`;
- `VpnService`, `Backend`, `TunnelManager` и текущий lifecycle подключения;
- локальный ручной импорт для expert/debug режима.

Android получает готовый профиль от control plane и сохраняет его через
существующий config store. Включение выполняется тем же штатным кодом, что и у
импортированного вручную туннеля.

## Целевая архитектура

```text
Android Cif VPN
  | HTTPS + certificate pinning
  v
Control API
  |-- Identity / sessions
  |-- Devices / public keys
  |-- Entitlements / subscriptions
  |-- Node assignment
  |-- Referral and bonus ledger
  v
PostgreSQL + Redis
  |
  | mTLS/private management network
  v
Node Agent ---- AmneziaWG node
```

### Android

- Генерирует private key локально; private key никогда не отправляется в API.
- Регистрирует только public key и метаданные устройства.
- Получает server peer, адрес, DNS, маршруты и AmneziaWG-параметры.
- Собирает локальную конфигурацию и передаёт её существующему config store.
- Хранит access/refresh credentials через Android Keystore-backed storage.
- Не считает ответ клиента доказательством оплаты или права доступа.

### Control API

- Единственный источник истины для entitlement пользователя.
- Идемпотентно регистрирует устройства и ограничивает их число по тарифу.
- Выбирает только healthy node с доступной capacity.
- Возвращает короткоживущий profile lease, а не бессрочный общий конфиг.
- Не хранит private keys пользователей и историю посещённых адресов.

### Node Agent

- Не доступен из публичного клиентского API.
- Получает подписанные команды add/remove/disable peer.
- Публикует агрегированные health/capacity metrics без истории трафика.
- Не принимает SSH-данные или административные команды от Android-клиента.

## Основные сущности

- `users`: аккаунт и состояние блокировки.
- `devices`: устройство, public key, platform, revoked_at.
- `plans`: ограничения устройств и период подписки.
- `subscriptions`: источник оплаты, состояние, paid_until.
- `entitlements`: итоговое право доступа, рассчитанное сервером.
- `vpn_nodes`: location, endpoint, capacity, health, protocol metadata.
- `device_assignments`: устройство, узел, tunnel address, lease expiry.
- `referral_codes`: стабильный код приглашающего.
- `referral_relations`: inviter/invitee и fraud state.
- `bonus_ledger`: неизменяемые операции начисления/отмены бонусных дней.
- `audit_log`: административные изменения без секретов и traffic logs.

## Правила безопасности

1. Private key создаётся и остаётся на устройстве.
2. API никогда не возвращает private key или SSH credential.
3. Refresh token ротируется; повторное использование старого токена отзывает
   всю token family.
4. Выдача профиля требует активный entitlement и неотозванное устройство.
5. Profile response не логируется целиком на клиенте или сервере.
6. Node agent и control API используют отдельные identities и mTLS.
7. Bonus ledger append-only; бонус нельзя менять прямым обновлением баланса.
8. Платёж подтверждается только серверной проверкой провайдера.

## Этапы реализации

### Phase 0 — contracts and safety baseline (этот этап)

- Архитектурная граница зафиксирована этим документом.
- API v1 зафиксирован в `openapi/cif-control-api-v1.yaml`.
- Runtime Android и tunnel engine не изменяются.

### Phase 1 — Android commercial shell

- Новый пакет `org.amnezia.awg.commercial` только в модуле `:ui`.
- Interfaces: `AuthRepository`, `DeviceRepository`, `ProfileRepository`.
- Encrypted credential store и API client.
- Feature flag: manual mode остаётся default до успешной интеграции.
- Contract tests на разбор profile response без запуска VPN.

### Phase 2 — Control API MVP

- PostgreSQL migrations, authentication, devices, entitlements, nodes.
- Идемпотентная регистрация устройства.
- Node selection и profile lease.
- Админские endpoints отделены от публичного API.

### Phase 3 — node agent and provisioning

- mTLS registration агента.
- Add/remove peer, health heartbeat, capacity metrics.
- Автоматическое назначение резервного узла.

### Phase 4 — billing, referrals and admin

- Server-side purchase verification.
- Referral relation и append-only bonus ledger.
- Admin UI с RBAC и audit log.

## Критерии готовности первой коммерческой беты

- Ни один private key не покидает устройство.
- Пользователь получает профиль без ручного импорта.
- Отозванное устройство перестаёт получать lease и удаляется с узла.
- Истёкший entitlement блокирует новую выдачу/продление профиля.
- Один упавший node не делает сервис полностью недоступным.
- Существующий ручной импорт и подключение проходят regression test.
- Debug APK и unit tests зелёные в CI.

## Обнаруженные технические долги

- Package/application id всё ещё `org.amnezia.awg`; менять его следует отдельной
  миграцией, иначе обновление установленного APK и хранилище конфигов сломаются.
- Часть Cif-строк в Kotlin-файлах имеет признаки повреждённой кодировки.
- Smart bypass содержит два расходящихся списка package names.
- Текущий dashboard создаёт coroutine на строку RecyclerView и раз в секунду
  читает config; это нужно упростить до одного lifecycle-aware state producer.
- Версия приложения остаётся `2.0.1` исходного проекта и не отражает Cif release.
- CI собирает debug APK, но пока не запускает lint/tests и не публикует SBOM.

