#!/bin/bash
# KECE 停止脚本：结束后端/判题机/前端三个本地进程。
# judge-mysql / redis 容器与原生 MySQL 保留。

pkill -x judger 2>/dev/null || true
pkill -f 'mvnw spring-boot:run' 2>/dev/null || true
pkill -f 'vite' 2>/dev/null || true

echo "已停止后端/判题机/前端。容器与原生 MySQL 保留。"
