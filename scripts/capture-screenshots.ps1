# ShopFlow — README ekran goruntuleri (Playwright)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$health = "http://localhost:8080/"
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $r = Invoke-WebRequest -Uri $health -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { $ready = $true; break }
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $ready) {
    Write-Host "Sunucu yok. Once: .\mvnw.cmd spring-boot:run"
    exit 1
}

$scriptDir = Join-Path $root "scripts"
if (-not (Test-Path (Join-Path $scriptDir "node_modules\playwright"))) {
    Push-Location $scriptDir
    if (-not (Test-Path "package.json")) {
        @'
{"name":"shopflow-screenshots","private":true,"type":"module","dependencies":{"playwright":"^1.49.0"}}
'@ | Set-Content package.json -Encoding UTF8
    }
    npm install --silent
    npx playwright install chromium
    Pop-Location
}

Push-Location $scriptDir
node capture-screenshots.mjs
Pop-Location
Write-Host "Screenshots: docs/screenshots/"
