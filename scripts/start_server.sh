#!/bin/bash
set -e

REGION=ap-northeast-2
# IMDSv2 토큰 발급 후 EC2 메타데이터에서 계정 ID 조회 (STS 불필요)
TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 60")
ACCOUNT_ID=$(curl -s -H "X-aws-ec2-metadata-token: ${TOKEN}" \
  http://169.254.169.254/latest/dynamic/instance-identity/document | jq -r .accountId)
ECR_URL="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
IMAGE="${ECR_URL}/sofit-user-api:latest"

echo ">>> ECR 로그인..."
aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL}

echo ">>> Secrets Manager에서 환경변수 로드..."
SECRET=$(aws secretsmanager get-secret-value \
  --secret-id sofit/user-api/prod \
  --region ${REGION} \
  --query SecretString \
  --output text)

DB_HOST=$(echo $SECRET | jq -r '.DB_HOST')
DB_USERNAME=$(echo $SECRET | jq -r '.DB_USERNAME')
DB_PASSWORD=$(echo $SECRET | jq -r '.DB_PASSWORD')
REDIS_HOST=$(echo $SECRET | jq -r '.REDIS_HOST')
REDIS_PORT=$(echo $SECRET | jq -r '.REDIS_PORT')
EXTERNAL_MOCK_URL=$(echo $SECRET | jq -r '.EXTERNAL_MOCK_URL')
STORAGE_BASE_URL=$(echo $SECRET | jq -r '.STORAGE_BASE_URL')
CODEF_CLIENT_ID=$(echo $SECRET | jq -r '.CODEF_CLIENT_ID')
CODEF_CLIENT_SECRET=$(echo $SECRET | jq -r '.CODEF_CLIENT_SECRET')
CODEF_BASE_URL=$(echo $SECRET | jq -r '.CODEF_BASE_URL')
CODEF_OAUTH_URL=$(echo $SECRET | jq -r '.CODEF_OAUTH_URL')
AI_SERVER_URL=$(echo $SECRET | jq -r '.AI_SERVER_URL')
AUDIT_ENABLED=$(echo $SECRET | jq -r '.AUDIT_ENABLED')

echo ">>> 이미지 pull: ${IMAGE}"
docker pull ${IMAGE}

echo ">>> 기존 컨테이너 정리..."
docker stop sofit-user-api 2>/dev/null || true
docker rm   sofit-user-api 2>/dev/null || true

mkdir -p /var/log/sofit

echo ">>> 컨테이너 실행..."
docker run -d \
  --name sofit-user-api \
  --restart unless-stopped \
  -p 8080:8080 \
  -v /var/log/sofit:/app/logs \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST="${DB_HOST}" \
  -e DB_USERNAME="${DB_USERNAME}" \
  -e DB_PASSWORD="${DB_PASSWORD}" \
  -e REDIS_HOST="${REDIS_HOST}" \
  -e REDIS_PORT="${REDIS_PORT}" \
  -e SPRING_DATA_REDIS_SSL_ENABLED=true \
  -e SPRING_SESSION_REDIS_CONFIGURE_ACTION=none \
  -e EXTERNAL_MOCK_URL="${EXTERNAL_MOCK_URL}" \
  -e STORAGE_BASE_URL="${STORAGE_BASE_URL}" \
  -e CODEF_CLIENT_ID="${CODEF_CLIENT_ID}" \
  -e CODEF_CLIENT_SECRET="${CODEF_CLIENT_SECRET}" \
  -e CODEF_BASE_URL="${CODEF_BASE_URL}" \
  -e CODEF_OAUTH_URL="${CODEF_OAUTH_URL}" \
  -e AI_SERVER_URL="${AI_SERVER_URL}" \
  -e AUDIT_ENABLED="${AUDIT_ENABLED}" \
  -e SPRING_PROFILES_ACTIVE=dev \
  ${IMAGE}

echo ">>> 배포 완료."
