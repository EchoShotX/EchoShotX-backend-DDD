#!/bin/bash

echo "=========================================="
echo "Validating deployment..."
echo "=========================================="

# 프로젝트 디렉토리로 이동
cd /home/ubuntu/echoshotx

# 컨테이너 상태 확인
RUNNING_CONTAINERS=$(docker-compose ps | grep "Up" | wc -l)

if [ "$RUNNING_CONTAINERS" -lt 3 ]; then
    echo "❌ ERROR: Not all containers are running"
    docker-compose ps
    docker-compose logs --tail=50
    exit 1
fi

echo "✅ All containers are running"

# 애플리케이션 헬스체크
MAX_RETRY=10
RETRY_COUNT=0
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"

echo "Checking application health..."

while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_CHECK_URL)

    if [ "$HTTP_STATUS" -eq 200 ]; then
        echo "✅ Application health check passed (HTTP $HTTP_STATUS)"

        # 헬스체크 상세 정보 출력
        echo ""
        echo "Health Check Details:"
        curl -s $HEALTH_CHECK_URL | python3 -m json.tool || curl -s $HEALTH_CHECK_URL

        echo ""
        echo "=========================================="
        echo "✅ Deployment validation successful!"
        echo "=========================================="
        exit 0
    fi

    RETRY_COUNT=$((RETRY_COUNT+1))
    echo "⏳ Health check attempt $RETRY_COUNT/$MAX_RETRY (HTTP $HTTP_STATUS) - Retrying in 5 seconds..."
    sleep 5
done

echo "❌ Application health check failed after $MAX_RETRY attempts"
echo ""
echo "Recent application logs:"
docker-compose logs --tail=50 app

echo "=========================================="
echo "❌ Deployment validation failed!"
echo "=========================================="
exit 1

