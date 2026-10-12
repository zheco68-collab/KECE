# KECE 一键启动脚本 (Windows PowerShell)
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path "$ScriptDir\.."

Write-Host "Starting KECE services..." -ForegroundColor Cyan
docker compose -f "$ScriptDir\docker-compose.yml" up -d --build

Write-Host ""
Write-Host "Services started:" -ForegroundColor Green
Write-Host "  Backend API:  http://localhost:8080/api/v1"
Write-Host "  Swagger UI:   http://localhost:8080/api/v1/swagger-ui.html"
Write-Host "  Frontend:     http://localhost:5173 (run 'npm run dev' in frontend/)"
Write-Host ""
Write-Host "To stop:  .\stop.ps1"
Write-Host "To logs:  docker compose -f deploy/docker-compose.yml logs -f"
