# KECE 停止脚本 (Windows PowerShell)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Stopping KECE services..." -ForegroundColor Cyan
docker compose -f "$ScriptDir\docker-compose.yml" down

Write-Host "Services stopped." -ForegroundColor Green
