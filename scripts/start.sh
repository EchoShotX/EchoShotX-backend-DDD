#!/bin/bash

echo "=========================================="
echo "Starting application..."
echo "=========================================="

# 프로젝트 디렉토리로 이동
cd /home/ubuntu/echoshotx

# .env 파일 로드
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Environment variables loaded"
else
    echo "❌ ERROR: .env file not found!"
    exit 1
fi

# Docker Compose로 컨테이너 시작
echo "Starting containers with Docker Compose..."
docker-compose up -d

if [ $? -eq 0 ]; then
    echo "✅ Containers started successfully"

    # 컨테이너 상태 확인
    echo ""
    echo "Container Status:"
    docker-compose ps
else
    echo "❌ Failed to start containers"
    exit 1
fi

# 애플리케이션 시작 대기
echo ""
echo "Waiting for application to start (60 seconds)..."
sleep 60

echo "=========================================="
echo "Start process completed"
echo "=========================================="

