# PROGRESS.md

## 현재 상태: CI/CD 파이프라인 구축 완료
- `deploy.yml` 작성 완료 (오타 수정 완료)
- `docker-compose.prod.yml` 작성 완료
- Repository Secrets 등록 완료 (`GHCR_TOKEN`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- 파이프라인 동작 테스트 성공 — main push 시 자동 배포 확인됨

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

## 해결한 이슈들

### 빌드 실패 — 테스트 시 DB 연결 오류
- GitHub 러너에 MySQL이 없어서 `contextLoads()` 테스트 실패
- 해결: `build.gradle`에 `testRuntimeOnly 'com.h2database:h2'` 추가 + `src/test/resources/application.yml`에 H2 인메모리 DB 설정

### 포트 충돌 — 3306 포트 점유
- NCP 서버에서 기존 MariaDB Docker 컨테이너가 3306 포트 점유 중이었음
- 해결: 기존 MariaDB 컨테이너 중지/삭제 후 재배포

### DB 접속 거부 — forcicd_app 사용자 미존재
- `docker-compose.prod.yml`에 `MYSQL_ROOT_PASSWORD`만 설정하여 root 계정만 존재
- 해결: MySQL 컨테이너에 접속하여 `forcicd_app` 사용자 수동 생성

### 서버 메모리 부족 — 1GB RAM
- MySQL 8.0 + Spring Boot 동시 실행 시 서버 멈춤
- 해결: NCP 인스턴스를 2GB RAM으로 새로 생성

## 미완/권장 사항
- `docker-compose.prod.yml`에 `MYSQL_USER`, `MYSQL_PASSWORD` 환경변수 추가 권장 (볼륨 초기화 시 수동 사용자 생성 방지)
- `actions/create-release@v1`의 `set-output` 경고 → 추후 `softprops/action-gh-release@v2`로 교체 고려

## 다음 할 일
- 프로젝트 기능 개발 진행

## 참고: 코드 작성 규칙
- Claude는 `.md` 파일 외에는 직접 생성/수정하지 않음
- 코드 스니펫을 보여주면 사용자가 직접 타이핑하며 작성
