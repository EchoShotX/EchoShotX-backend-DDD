#!/bin/bash

echo "=========================================="
echo "Starting application..."
echo "=========================================="

ROOT_DIR="/home/ubuntu/echoshotx"

resolve_env_file() {
    local base_dir="$1"
    local candidates=(
        "${base_dir}/.env"
        "${base_dir}/EchoShotX-backend-private/.env"
    )

    for env_path in "${candidates[@]}"; do
        if [ -f "$env_path" ]; then
            echo "$env_path"
            return 0
        fi
    done

    return 1
}

# 프로젝트 디렉토리로 이동
cd "$ROOT_DIR"

ENV_FILE=$(resolve_env_file "$ROOT_DIR")

if [ -z "$ENV_FILE" ]; then
    echo "❌ ERROR: .env file not found at $ROOT_DIR/.env or $ROOT_DIR/EchoShotX-backend-private/.env"
    exit 1
fi

# .env 파일 로드
set -o allexport
. "$ENV_FILE"
set +o allexport

echo "✅ Environment variables loaded from $ENV_FILE"

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

