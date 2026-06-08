# Render tek-tik deploy — tarayiciyi acar
$repo = "https://github.com/sametozlu/javaproject"
$deployUrl = "https://render.com/deploy?repo=$([uri]::EscapeDataString($repo))"

Write-Host ""
Write-Host "  ShopFlow -> Render deploy"
Write-Host "  -------------------------"
Write-Host "  Tarayici aciliyor..."
Write-Host ""
Write-Host "  Senden tek istenen:"
Write-Host "  1) Render'a GitHub ile gir (ilk seferde)"
Write-Host "  2) 'Apply' veya 'Deploy Blueprint' tikla"
Write-Host ""
Write-Host "  Canli link (deploy sonrasi):"
Write-Host "  https://shopflow-api.onrender.com"
Write-Host ""

Start-Process $deployUrl
