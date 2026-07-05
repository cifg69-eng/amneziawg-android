Cif VPN — Phase 2 clean working UI

Что исправлено:
1. Убран фейковый декоративный большой круг, который не управлял VPN.
2. Убран огромный сломанный блок из main_activity.xml.
3. Брендинг перенесён в нормальный компактный верхний блок внутри списка туннелей.
4. Реальный переключатель туннеля остаётся штатным: он использует официальный TunnelListFragment/BaseFragment и запускает настоящий AmneziaWG backend.
5. Название приложения остаётся Cif VPN.

Как применить:
1. Распакуй архив в корень проекта:
   C:\Users\kufik\Documents\GitHub\amneziawg-android
2. Согласись на замену файлов.
3. GitHub Desktop -> Commit message: Cif VPN phase 2 clean working UI
4. Commit -> Push origin.
5. В GitHub Actions дождись APK.
