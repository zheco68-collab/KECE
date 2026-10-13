# KECE 停止脚本：结束后端/判题机/前端三个本地进程。
# judge-mysql / redis 容器与原生 MySQL 保留（它们是常驻基础设施，随 Docker 重启自动拉起）。

Stop-Process -Name judger -Force -ErrorAction SilentlyContinue

foreach ($Port in 8080, 5173) {
    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
}

Write-Host "已停止后端(8080)/判题机/前端(5173)。容器与原生 MySQL 保留。" -ForegroundColor Green
