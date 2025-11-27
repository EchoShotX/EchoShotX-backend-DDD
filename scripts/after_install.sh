#!/bin/bash

echo "=========================================="
echo "Running after install script..."
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
    echo "Please supply the required environment file (it can live in the private submodule)"
    exit 1
fi

# 환경변수를 임시로 로드 (MySQL 설치/설정에 사용)
set -o allexport
. "$ENV_FILE"
set +o allexport

install_local_mysql() {
    if [ -z "$DB_NAME" ] || [ -z "$DB_USERNAME" ] || [ -z "$DB_PASSWORD" ]; then
        echo "❌ ERROR: DB_NAME/DB_USERNAME/DB_PASSWORD must be set for local MySQL setup"
        exit 1
    fi

    if ! command -v mysql >/dev/null 2>&1; then
        echo "Installing MySQL server (Ubuntu)..."
        apt-get update
        DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server
    else
        echo "✅ MySQL already installed"
    fi

    MY_CNF="/etc/mysql/mysql.conf.d/mysqld.cnf"
    if [ -f "$MY_CNF" ]; then
        if grep -q "^bind-address" "$MY_CNF"; then
            sed -i "s/^bind-address\\s*=\\s*.*/bind-address = 0.0.0.0/" "$MY_CNF"
        else
            echo "bind-address = 0.0.0.0" >> "$MY_CNF"
        fi
    fi

    systemctl enable --now mysql

    db_user_escaped=${DB_USERNAME//\'/\'\'}
    db_password_escaped=${DB_PASSWORD//\'/\'\'}

    echo "Configuring database ${DB_NAME} and user ${DB_USERNAME}..."
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    mysql -u root -e "CREATE USER IF NOT EXISTS '${db_user_escaped}'@'%' IDENTIFIED BY '${db_password_escaped}';"
    mysql -u root -e "ALTER USER '${db_user_escaped}'@'%' IDENTIFIED BY '${db_password_escaped}';"
    mysql -u root -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${db_user_escaped}'@'%';"
    mysql -u root -e "FLUSH PRIVILEGES;"
}

install_local_mysql

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

