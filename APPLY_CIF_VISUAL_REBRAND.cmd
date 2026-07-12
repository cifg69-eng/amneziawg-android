@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_CIF_VISUAL_REBRAND.ps1"
if errorlevel 1 (
  echo.
  echo Ошибка применения брендинга.
  pause
  exit /b 1
)
echo.
echo Готово. Теперь откройте GitHub Desktop, сделайте Commit и Push.
pause
