#!/bin/bash

echo "=========================================="
echo "Running before install script..."
echo "=========================================="

# 프로젝트 디렉토리 생성
if [ ! -d /home/ubuntu/echoshotx ]; then
    mkdir -p /home/ubuntu/echoshotx
    echo "✅ Created project directory"
fi

# 로그 디렉토리 생성
if [ ! -d /home/ubuntu/echoshotx/logs ]; then
    mkdir -p /home/ubuntu/echoshotx/logs
    echo "✅ Created logs directory"
fi

# 이전 배포 파일 백업 (선택사항)
if [ -d /home/ubuntu/echoshotx/backup ]; then
    rm -rf /home/ubuntu/echoshotx/backup
fi

if [ -f /home/ubuntu/echoshotx/docker-compose.yml ]; then
    mkdir -p /home/ubuntu/echoshotx/backup
    cp /home/ubuntu/echoshotx/docker-compose.yml /home/ubuntu/echoshotx/backup/ || true
    echo "✅ Backed up previous deployment files"
fi

echo "=========================================="
echo "Before install completed"
echo "=========================================="

