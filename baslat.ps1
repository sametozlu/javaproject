# ShopFlow — tek komutla çalıştır (ayar gerekmez)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env" -ErrorAction SilentlyContinue
    if (-not (Test-Path ".env")) {
        @"
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=shopflow-dev-secret-key-min-32-chars
APP_URL=http://localhost:8080
APP_MAIL_ENABLED=true
MAIL_HOST=localhost
MAIL_PORT=1025
STRIPE_SECRET_KEY=
STRIPE_PUBLISHABLE_KEY=
"@ | Set-Content ".env" -Encoding UTF8
    }
}

Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $i = $line.IndexOf("=")
    if ($i -lt 1) { return }
    $name = $line.Substring(0, $i).Trim()
    $val = $line.Substring($i + 1).Trim()
    Set-Item -Path "Env:$name" -Value $val
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "[*] MailHog baslatiliyor (e-posta testi)..."
    docker compose up -d mailhog 2>$null
}

Write-Host ""
Write-Host "  ShopFlow magaza : http://localhost:8080"
Write-Host "  Admin giris     : admin@shop.com / admin123"
Write-Host "  E-posta kutusu  : http://localhost:8025 (MailHog)"
Write-Host "  Durdurmak icin  : Ctrl+C"
Write-Host ""

& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
