# ============================================
# KoSpot Backend - Production Environment
# ============================================

# -------------------- Database (RDS) --------------------
DB_HOST= RDS_ENDPOINT
DB_PORT=3306
DB_NAME=RDS_DB_NAME
DB_USERNAME=DB_USERNAME
DB_PASSWORD=DB_PASSWORD

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

# -------------------- AWS S3 Buckets --------------------
# 영상 저장용 버킷 이름
S3_BUCKET=

# -------------------- Application --------------------
SERVER_PORT=8080
# SPRING_PROFILES_ACTIVE=prod

# -------------------- WebSocket --------------------
WEBSOCKET_ALLOWED_ORIGINS= 필요한 경우 입력

