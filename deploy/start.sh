#!/bin/bash
# KECE 一键启动脚本 (Linux/macOS)
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Starting KECE services..."
docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d --build

echo ""
echo "Services started:"
echo "  Backend API:  http://localhost:8080/api/v1"
echo "  Swagger UI:   http://localhost:8080/api/v1/swagger-ui.html"
echo "  Frontend:     http://localhost:5173 (run 'npm run dev' in frontend/)"
echo ""
echo "To stop:  ./stop.sh"
echo "To logs:  docker compose -f deploy/docker-compose.yml logs -f"
