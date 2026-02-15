# PROGRESS.md

## 현재 상태
- `deploy.yml` 작성 완료 (오타 수정 필요 — 아래 참고)
- `docker-compose.prod.yml` **아직 미작성**

## deploy.yml 수정 필요 사항 (3개)
1. `name: CI/CI Pipeline` → `name: CI/CD Pipeline` (오타)
2. `uses: docker/logint-action@v3` → `uses: docker/login-action@v3` (오타)
3. `password: ${{ SECRETS.GHCR_TOKEN }}` → `password: ${{ secrets.GHCR_TOKEN }}` (대소문자)

## deploy.yml 확인 필요 사항 (2개)
1. paths 필터에 `docker-compose.yml`이 아닌 `docker-compose.prod.yml`이 들어가야 함
2. `./gradlew build -x test`에서 `-x test`(테스트 스킵) 제거 여부 결정 필요

## 결정된 사항

### 워크플로우 구조 (4개 Job)
1. **build** — GitHub 러너에서 Java 17 설치 → `./gradlew build` + Gradle 캐시
2. **tag** — 자동 버전 태그 생성 + GitHub Release
3. **docker** — Docker 이미지 빌드 → GHCR 푸시 (버전 태그 + latest)
4. **deploy** — self-hosted runner(라벨 `work-1`)에서 `docker compose`로 배포

### 배포 방식
- `docker compose` 선택 (DB + 앱 함께 관리)
- 기존 `docker-compose.yml`은 로컬 개발용 유지, `docker-compose.prod.yml`을 별도 생성
- app 서비스: `build:` 대신 `image: ghcr.io/yeboong99/forcicd:latest`

### 인증
- GHCR 로그인에 `GHCR_TOKEN` (classic token, 이미 Secret 등록됨) 사용

### GitHub 정보
- 계정: `yeboong99`
- Runner 라벨: `work-1`
- GHCR 이미지 경로: `ghcr.io/yeboong99/forcicd`

## 강의 워크플로우에서 적용 여부 검토 결과

### 적용한 것
1. **트리거 paths 필터** — 불필요한 파이프라인 실행 방지
2. **Gradle 캐시** — `setup-java`에 `cache: gradle` 추가
3. **tag Job** — 자동 버전 태그/릴리즈 생성

### 적용하지 않은 것
- **artifact 전달** — Dockerfile multi-stage build에서 자체 빌드하므로 불필요
- **docker run 배포** — docker compose 방식 채택

## 다음 할 일
1. deploy.yml 오타/수정사항 반영
2. `docker-compose.prod.yml` 작성
3. Repository Secrets 등록 확인 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
4. main에 push하여 파이프라인 동작 테스트

## 참고: 코드 작성 규칙
- Claude는 `.md` 파일 외에는 직접 생성/수정하지 않음
- 코드 스니펫을 보여주면 사용자가 직접 타이핑하며 작성
