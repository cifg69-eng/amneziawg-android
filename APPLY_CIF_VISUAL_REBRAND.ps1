param()

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ResRoot = Join-Path $RepoRoot "ui\src\main\res"

if (-not (Test-Path $ResRoot)) {
    throw "Не найден каталог ui\src\main\res. Распакуйте архив в корень репозитория."
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$files = Get-ChildItem -Path $ResRoot -Recurse -File -Filter "*.xml" |
    Where-Object {
        $_.Directory.Name -like "values*" -and
        ((Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8) -match "<string|<plurals")
    }

$changed = 0

foreach ($file in $files) {
    $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    $before = $text

    # Visible product branding only. Resource identifiers, packages and engine code are not touched.
    $text = [regex]::Replace($text, "Amnezia\s*WG", "Cif VPN", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    $text = [regex]::Replace($text, "Amnezia", "Cif VPN", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)

    # Remove the former provider/site advertisement from the import warning.
    if ($file.Directory.Name -eq "values-ru") {
        $replacement = '<string name="import_disclaimer">Используйте только VPN-конфигурации, полученные из надёжного источника.</string>'
    } else {
        $replacement = '<string name="import_disclaimer">Use only VPN configurations received from a trusted source.</string>'
    }

    $text = [regex]::Replace(
        $text,
        '<string\s+name="import_disclaimer"[^>]*>.*?</string>',
        $replacement,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )

    # Make the public app name deterministic even if upstream resources change.
    $text = [regex]::Replace(
        $text,
        '<string\s+name="app_name"[^>]*>.*?</string>',
        '<string name="app_name" translatable="false">Cif VPN</string>',
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )

    if ($text -ne $before) {
        [System.IO.File]::WriteAllText($file.FullName, $text, $utf8NoBom)
        $changed++
        Write-Host "Обновлено: $($file.FullName.Substring($RepoRoot.Length + 1))"
    }
}

$remaining = @()
foreach ($file in $files) {
    $matches = Select-String -LiteralPath $file.FullName -Pattern "Amnezia|amnezia\.org" -AllMatches
    if ($matches) {
        $remaining += $matches
    }
}

if ($remaining.Count -gt 0) {
    Write-Host ""
    Write-Host "Остались видимые упоминания:" -ForegroundColor Red
    $remaining | ForEach-Object { Write-Host $_.Path ":" $_.LineNumber ":" $_.Line }
    throw "Ребрендинг строк не завершён."
}

Write-Host ""
Write-Host "Cif VPN branding applied successfully." -ForegroundColor Green
Write-Host "Изменено файлов строк: $changed"
Write-Host "VPN engine, package org.amnezia.awg, keys and tunnel logic were not changed."
