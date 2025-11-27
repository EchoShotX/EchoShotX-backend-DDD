# ============================================
# EchoShotX Backend - Production Environment
# ============================================

# `.env`는 다음 위치에서 자동으로 검색됩니다:
# 1. 리포지토리 루트 (`.env`)
# 2. 서브모듈 (`EchoShotX-backend-private/.env`)
# 서브모듈에만 비밀 파일이 있다면 이 문서에 적힌 위치에 두기만 하면 됩니다.


# -------------------- Database (RDS) --------------------
# DB_HOST= RDS_ENDPOINT
# DB_PORT=3306
# DB_NAME=RDS_DB_NAME
# DB_USERNAME=DB_USERNAME
# DB_PASSWORD=DB_PASSWORD

# -------------------- Local MySQL on EC2 (Ubuntu)
# 도커 컨테이너가 EC2 호스트에서 실행 중인 MySQL에 접속하게 하려면 아래처럼 설정합니다.
# `scripts/after_install.sh`가 MySQL 설치/설정, `extra_hosts`가 호스트 연결을 담당합니다.
DB_HOST=host.docker.internal
DB_PORT=3306
DB_NAME=echoshotx
DB_USERNAME=echoshotx
DB_PASSWORD=1234

# `scripts/after_install.sh`에서 Ubuntu용 MySQL 서버를 설치/구동하고 `bind-address=0.0.0.0`로 설정합니다.
# `docker-compose.yml`의 `extra_hosts` 항목이 컨테이너에서 `host.docker.internal` 이름으로 EC2의 MySQL에 닿게 해줍니다.




# -------------------- Redis --------------------
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=1234

# -------------------- JWT --------------------
JWT_SECRET=JWT_SECRET 하드코딩

# -------------------- OAuth2 --------------------
OAUTH_GOOGLE_CLIENT_ID=
OAUTH_GOOGLE_CLIENT_SECRET=
OAUTH_NAVER_CLIENT_ID=
OAUTH_NAVER_CLIENT_SECRET=
OAUTH_KAKAO_CLIENT_ID=
OAUTH_KAKAO_CLIENT_SECRET=
AUTH_SUCCESS_REDIRECT_URI= 성공시 콜백 링크
# -------------------- AWS Configuration --------------------
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_REGION=ap-northeast-2

# -------------------- SQS Queues --------------------
AWS_SQS_QUEUE_URL=
AWS_SQS_MESSAGE_GROUP_ID=KoSpotGroup <- 임의 값

주의사항: AWS 계정과 SQS 생성자 동일이어야함

# -------------------- AWS S3 Buckets --------------------
# 영상 저장용 버킷 이름
S3_BUCKET=

# -------------------- Application --------------------
SERVER_PORT=8080

# -------------------- WebSocket --------------------
WEBSOCKET_ALLOWED_ORIGINS= 필요한 경우 입력

