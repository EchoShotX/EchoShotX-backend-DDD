#!/bin/bash

echo "=========================================="
echo "Running after install script..."
echo "=========================================="

# 프로젝트 디렉토리로 이동
cd /home/ubuntu/echoshotx

# 환경변수 파일 확인
if [ ! -f .env ]; then
    echo "❌ ERROR: .env file not found!"
    echo "Please create .env file with required environment variables"
    exit 1
fi

# Docker 이미지 로드
if [ -f echoshotx-backend.tar ]; then
    echo "Loading Docker image..."
    docker load -i echoshotx-backend.tar

    if [ $? -eq 0 ]; then
        echo "✅ Docker image loaded successfully"
        # 이미지 tar 파일 삭제 (디스크 공간 절약)
        rm -f echoshotx-backend.tar
    else
        echo "❌ Failed to load Docker image"
        exit 1
    fi
else
    echo "❌ Docker image file not found!"
    exit 1
fi

# Docker Compose 파일 권한 설정
chmod 644 docker-compose.yml

# 스크립트 실행 권한 설정
chmod +x scripts/*.sh

echo "=========================================="
echo "After install completed"
echo "=========================================="

