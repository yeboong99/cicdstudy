# 배포 실패 시 최소 롤백 절차

## 목적
배포 직후 장애가 발생했을 때, 직전 정상 이미지 태그로 빠르게 되돌린다.

## 전제
- 대상 EC2 인스턴스: `/home/ubuntu/app`
- 배포 시 `.env` 파일에 아래 키가 유지됨
  - `APP_IMAGE_TAG` (현재 배포 태그)
  - `PREVIOUS_APP_IMAGE_TAG` (직전 배포 태그)

## 1) 서버에서 즉시 롤백
```bash
set -Eeuo pipefail
cd /home/ubuntu/app

PREV_TAG="$(grep -E '^PREVIOUS_APP_IMAGE_TAG=' .env | tail -n 1 | cut -d= -f2- || true)"
if [ -z "${PREV_TAG}" ]; then
  echo "PREVIOUS_APP_IMAGE_TAG 값이 없어 롤백할 태그를 찾을 수 없습니다."
  exit 1
fi

if grep -q '^APP_IMAGE_TAG=' .env; then
  sed -i "s/^APP_IMAGE_TAG=.*/APP_IMAGE_TAG=${PREV_TAG}/" .env
else
  echo "APP_IMAGE_TAG=${PREV_TAG}" >> .env
fi

docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d --remove-orphans
```

## 2) 확인
```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs app --tail=100
```
- 앱 헬스체크/주요 API 응답 확인
- 장애가 해소되면 원인 분석 후 다음 배포 진행

## 3) `PREVIOUS_APP_IMAGE_TAG`가 없는 경우
- GitHub Container Registry에서 마지막 정상 태그를 확인
- `.env`의 `APP_IMAGE_TAG`를 해당 태그로 수동 지정 후 재기동

```bash
cd /home/ubuntu/app
sed -i 's/^APP_IMAGE_TAG=.*/APP_IMAGE_TAG=<정상태그>/' .env
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d --remove-orphans
```
