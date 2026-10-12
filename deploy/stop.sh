#!/bin/bash
# KECE 停止脚本 (Linux/macOS)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Stopping KECE services..."
docker compose -f "$SCRIPT_DIR/docker-compose.yml" down

echo "Services stopped."
