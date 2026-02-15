# COMMUNICATED

## 대화 목적
- 기존 Spring Boot 프로젝트(`forcicd`)를 GitHub Actions와 NCP Ubuntu 서버에 연동해 CI/CD 파이프라인을 구축하려는 목표를 확인했다.

## 현재까지 사용자 측 준비 사항
- NCP VM(우분투)에 Docker 설치 완료.
- DB는 MySQL 대신 MariaDB로 테스트 설치 및 접속/계정/권한 실습 완료.
- GitHub Repository에서 self-hosted runner 설정 진행.
- GitHub classic token 발급 후 Repository Secret `GHCR_TOKEN` 생성 완료.

## 워크플로 초안 점검 결과
사용자가 가져온 `work-1.yml` 초안은 그대로 쓰기보다 프로젝트 기준 수정이 필요하다고 정리했다.

- Java 버전 불일치:
  - 초안: JDK 19
  - 프로젝트(`build.gradle`): Java 17
- 경로/트리거 보완 필요:
  - `readme.md` 대신 `README.md`(대소문자) 및 Gradle 관련 파일 트리거 보강 필요
- GHCR 인증:
  - `GITHUB_TOKEN` 대신 준비한 `GHCR_TOKEN` 사용 권장
- 배포 시 환경변수:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=prod` 주입 필요

## 제공한 개선 워크플로 방향
- `.github/workflows/deploy.yml`로 작성 진행 중.
- 구성 (4개 Job):
  - `build` (Gradle build + Gradle 캐시)
  - `tag` (자동 태그/릴리즈)
  - `docker` (GHCR 빌드/푸시, 버전 태그 + latest)
  - `deploy` (self-hosted runner에서 docker compose로 배포)
- `permissions`에 `contents: write`, `packages: write` 설정 포함.
- 트리거: main 브랜치 push + paths 필터 적용.

## Secrets 관련 안내
Repository Secrets에 아래 3개를 추가하라고 안내했다.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

예시:
- `DB_URL=jdbc:mysql://db:3306/forcicd?useSSL=false&serverTimezone=UTC` (docker compose 네트워크 내 서비스명 `db` 사용)

## Docker 내부 네트워크 관련 결론
- 앱 컨테이너와 DB 컨테이너가 같은 `docker-compose` 네트워크라면 서버 IP는 불필요.
- 서비스명으로 접속:
  - 예: `jdbc:mysql://db:3306/forcicd...`
- 서버 IP는 외부 클라이언트가 DB에 직접 붙을 때만 필요.
- 로컬에서 `docker compose`로 띄우면 `db`, IDE에서 직접 `bootRun`하면 `localhost`.
- 배포 환경은 docker compose이므로 `db`.

## MariaDB vs MySQL JDBC
- MariaDB를 사용해도 현재 드라이버(`mysql-connector-j`) 기준으로 `jdbc:mysql://...` URL은 일반적으로 호환 가능하다고 안내했다.
- 드라이버를 MariaDB 전용으로 바꿀 경우 `jdbc:mariadb://...` 사용이 권장된다.

## 계정/권한 네이밍 가이드
- 앱 전용 계정명으로 `forcicd_app` 사용 제안에 동의.
- 권장 SQL(개념):
  - DB: `forcicd`
  - USER: `forcicd_app@'%'`
  - 권한: `forcicd.*` 범위로 최소화 권장

## 프로필 적용 관련 핵심 결론
- `application-local.yml`, `application-prod.yml` 모두 `${DB_*}` 환경변수 기반으로 작성되어 있음을 확인했다.
- `prod` 프로필은 NCP라고 자동 적용되지 않는다.
- 반드시 실행 시 `SPRING_PROFILES_ACTIVE=prod`를 명시해야 한다.

## 배포 방식 결정
- `docker compose` 기반 배포로 확정.
- `docker-compose.yml`(로컬 개발용)과 `docker-compose.prod.yml`(배포용)을 분리.
  - 로컬: `build:` 사용 (코드에서 직접 빌드)
  - 배포: `image: ghcr.io/yeboong99/forcicd:latest` 사용 (GHCR에서 pull)
- 분리 이유: 배포 서버에서는 이미 GitHub 머신에서 빌드한 이미지를 GHCR에 올려놨으므로, 다시 빌드할 필요 없이 pull만 하면 됨.

## GitHub Actions 개념 정리 (사용자 학습 내용)

### 워크플로우 구조
- **Workflow**: `.github/workflows/` 안의 YAML 파일 하나가 워크플로우 하나. 자동화 시나리오 전체.
- **Job**: 워크플로우 안의 작업 묶음. 각 Job은 독립된 머신(러너)에서 실행.
- **Step**: Job 안에서 순서대로 실행되는 개별 명령.

### Runner
- **GitHub-hosted runner**: GitHub이 제공하는 클라우드 머신 (`ubuntu-latest`). build, tag, docker Job에 사용.
- **Self-hosted runner**: 직접 등록한 서버. NCP 서버가 이 역할 (라벨: `work-1`). deploy Job에 사용.
- Runner 라벨은 Job과 Runner를 매칭하는 키. 워크플로우 파일 이름과는 무관.

### Step 작성 방법
- `run:` — 쉘 명령어를 직접 실행 (예: `./gradlew build`)
- `uses:` — 미리 만들어진 Action(플러그인)을 사용 (예: `actions/checkout@v4`)

### Checkout
- `actions/checkout@v4`는 GitHub 저장소의 코드를 Runner 머신으로 복사해오는 Action.
- 각 Job은 빈 머신에서 시작하므로, 코드가 필요한 모든 Job에 Checkout이 필요.

### permissions
- 워크플로우가 GitHub 리소스에 접근하는 권한 설정.
- `contents: write`: 태그/릴리즈 생성에 필요.
- `packages: write`: GHCR 이미지 푸시에 필요.

### GHCR (GitHub Container Registry)
- Docker 이미지를 저장하는 GitHub의 저장소. Docker Hub의 GitHub 버전.
- `GHCR_TOKEN`은 이 저장소에 접근하기 위한 인증 토큰.

### Docker 이미지 태그
- 하나의 이미지에 이름표를 여러 개 붙일 수 있음 (이미지가 복사되는 것이 아님).
- `latest`: 항상 최신 이미지를 가리킴. 배포 시 사용.
- 버전 태그 (예: `v1.0.1`): 특정 시점에 고정. 롤백 시 사용.

### tag vs release
- **tag_name**: Git 커밋에 붙이는 버전 표시. 코드 레벨에서 사용 (`git checkout v1.0.1`).
- **release_name**: GitHub Releases 페이지에 표시되는 제목. 사람이 보기 위한 이름.

### artifact
- Job 간에 파일을 전달하는 수단 (upload-artifact → download-artifact).
- 본 프로젝트에서는 미사용: Dockerfile multi-stage build에서 자체 빌드하므로 build 결과물 전달 불필요.

### 환경변수 관리
- `.env` 파일: 로컬 개발용. Spring Boot가 직접 읽음.
- Repository Secrets: GitHub Actions 배포용. 워크플로우가 읽어서 컨테이너 환경변수로 전달.
- Spring Boot 입장에서는 둘 다 동일하게 환경변수로 들어옴. 값을 공급하는 출발점이 다를 뿐.
- 모든 `.env` 변수를 Secrets에 넣을 필요 없음. 배포 시 필요한 민감 정보만 등록.

### CI vs CD
- **CI (Continuous Integration)**: 코드 push 시 자동 빌드 + 테스트. (build Job)
- **CD (Continuous Deployment)**: 검증된 코드를 자동으로 서버에 배포. (tag → docker → deploy Job)

### Terraform (참고)
- 인프라를 코드로 관리하는 도구. 서버 생성, 네트워크 설정 등을 코드로 수행.
- Docker와 관리 대상이 다름: Terraform은 건물(서버)을 짓고, Docker는 건물 안에 가구(앱)를 배치.
- 클라우드 제공자의 API 키로 인증. Terraform 자체는 무료, 생성된 인프라에만 비용 발생.
- 서버 1대 규모에서는 불필요. 콘솔에서 수동 생성으로 충분.

## 미완/다음 작업
- deploy.yml 오타 수정 (3건) + 확인 사항 반영 (2건)
- `docker-compose.prod.yml` 작성
- Repository Secrets 등록 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- main에 push하여 파이프라인 동작 테스트
