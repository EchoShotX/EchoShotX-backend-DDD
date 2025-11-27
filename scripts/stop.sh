#!/bin/bash

echo "=========================================="
echo "Stopping existing containers..."
echo "=========================================="

# 프로젝트 디렉토리로 이동
cd /home/ubuntu/echoshotx

# Docker Compose로 컨테이너 중지 및 제거
if [ -f docker-compose.yml ]; then
    docker-compose down || true
    echo "✅ Containers stopped successfully"
else
    echo "⚠️ docker-compose.yml not found, skipping container stop"
fi

# 사용하지 않는 Docker 리소스 정리
echo "Cleaning up unused Docker resources..."
docker system prune -f || true

echo "=========================================="
echo "Stop process completed"
echo "=========================================="

