# PROGRESS.md

## 현재 상태: Terraform 도입 진행 중 (NCP → AWS 전환)

### Terraform 도입 계획 요약

**목표**: NCP에서 AWS로 전환하면서, 인프라를 Terraform으로 코드화. Docker Compose 유지. 배포 트리거를 self-hosted runner → **AWS SSM Send-Command**로 전환.

**배포 방식 변경 이유 (self-hosted runner → SSM)**
- EC2에 runner 프로세스를 상주시키지 않아 메모리 절약 (~200MB)
- runner 버전 업데이트, 토큰 재발급 등 유지보수 불필요
- SSM Agent는 Ubuntu AMI에 기본 포함 → 추가 설치 없음
- EC2 재생성 시 runner 재등록 불필요

**사전 준비 (사용자 직접 수행)**
1. `brew install terraform` + `brew install awscli`
2. AWS 계정 생성 → IAM 사용자(`forcicd-terraform`) 생성 → Access Key 발급
   - 정책: EC2, VPC, IAM, SSM FullAccess
3. `aws configure`로 자격 증명 등록 (리전: ap-northeast-2)
4. AWS 콘솔에서 SSH 키 페어(`forcicd-key`) 생성 → `~/.ssh/`에 저장

**Terraform 프로젝트 구조** (`infra/` 디렉토리)

| 파일 | 역할 |
|------|------|
| `provider.tf` | AWS 프로바이더 설정, Terraform 버전 요구사항 |
| `variables.tf` | 변수 선언 (리전, 인스턴스 타입, 키, DB 정보 등) |
| `main.tf` | VPC, 서브넷, 인터넷 게이트웨이, 보안 그룹, Elastic IP, EC2 인스턴스 |
| `iam.tf` | IAM 역할 + SSM 권한 (EC2가 SSM 명령을 받을 수 있도록) |
| `outputs.tf` | 출력 값 (Elastic IP, 인스턴스 ID, SSH 명령어, 앱 URL) |
| `userdata.sh` | EC2 초기화 (Docker 설치 + .env 생성. runner 설치 없음) |
| `terraform.tfvars` | 실제 변수 값 (gitignore 대상) |

**AWS 인프라 구성**
- VPC (10.0.0.0/16) + 퍼블릭 서브넷 (10.0.1.0/24)
- 보안 그룹: SSH(내 IP만), 8080(전체). 3306은 미개방 (Docker 내부 네트워크)
- EC2: Ubuntu 22.04, t3.small (2GB RAM), 20GB gp3
- Elastic IP로 고정 IP 보장
- IAM 역할: EC2에 SSM 권한 부여 (AmazonSSMManagedInstanceCore)

**CI/CD 변경사항**
- `deploy.yml`의 deploy Job 수정: `runs-on: ubuntu-latest` + SSM Send-Command
- GitHub Secrets 추가: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `EC2_INSTANCE_ID`
- NCP self-hosted runner 삭제

**tfstate 관리**: 로컬 (내 컴퓨터에 저장, 추후 S3로 전환 가능)

**예상 비용**: ~$21/월 (EC2 t3.small + EBS 20GB + Elastic IP)

**향후 무중단 배포 확장**: Docker Compose + Nginx Blue-Green 방식 권장. 보안그룹에 80번 포트 추가만으로 확장 가능.

**주의사항**
- `terraform destroy` 시 DB 데이터 소실 → 삭제 전 백업 필수
- tfstate 파일 분실 시 Terraform이 기존 인프라 인식 불가
- `my_ip` 변경 시 → `terraform.tfvars` 수정 후 `terraform apply`

---

## 이전 상태: CI/CD 배포 안정화 완료
- `deploy.yml` 작성 완료 (배포 디렉토리 고정 경로 방식으로 개선)
- `docker-compose.prod.yml` 작성 완료 (`restart: unless-stopped` 적용)
- Repository Secrets 등록: `GHCR_TOKEN` (배포 시 GHCR 로그인용)
- 환경변수(DB_URL 등)는 서버 `~/app/.env`에서 직접 관리 (GitHub Secrets 미사용)
- 파이프라인 동작 테스트 성공 — main push 시 자동 배포 확인됨
- 서버 수동 관리 가능 — `cd ~/app && docker compose -f docker-compose.prod.yml down/up -d`

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

## 해결한 이슈들 (2차)

### 서버 재시작 후 수동 관리 불가
- 서버 재시작 후 컨테이너는 존재하나 접속 불가, `docker compose down` 시 compose 파일 미발견
- 원인: GitHub Actions deploy job이 checkout 디렉토리에서 compose를 실행 → job 종료 후 파일 소실
- 해결: deploy job에서 `docker-compose.prod.yml`을 서버의 `~/app/`에 복사 후 실행하도록 변경

### 환경변수 관리 방식 변경
- 기존: GitHub Secrets에서 `env:` 블록으로 런타임 주입 → 서버 재시작 시 환경변수 소실
- 변경: 서버의 `~/app/.env`에 직접 관리 → compose가 자동으로 읽음
- deploy.yml에서 `env:` 블록(DB_URL, DB_USERNAME, DB_PASSWORD) 제거

### restart 정책 변경
- `restart: on-failure` → `restart: unless-stopped`로 변경
- 서버 재부팅 후에도 컨테이너가 자동 재시작됨

### DB 연결 실패 (.env의 DB_URL)
- `~/app/.env`에 `DB_URL=jdbc:mysql://localhost:3306/...`으로 작성하여 연결 실패
- Docker Compose 내부 네트워크에서는 `localhost`가 아닌 서비스명(`db`)을 사용해야 함
- `DB_URL=jdbc:mysql://db:3306/...`으로 수정하여 해결

## 미완/권장 사항
- `docker-compose.prod.yml`에 `MYSQL_USER`, `MYSQL_PASSWORD` 환경변수 추가 권장 (볼륨 초기화 시 수동 사용자 생성 방지)
- `actions/create-release@v1`의 `set-output` 경고 → 추후 `softprops/action-gh-release@v2`로 교체 고려

## 현재 진행 상황 (2026-02-19 기준)

### 완료된 사전 준비
- Terraform v1.5.7 + AWS CLI v2.33.25 설치 완료
- IAM 사용자 `forcicd-terraform` 생성 + Access Key 발급 + CSV 저장
- `aws configure` 완료 (리전: ap-northeast-2)
- SSH 키 페어 `forcicd-key` 생성 → `~/.ssh/forcicd-key.pem` 저장 (권한 400)
- 카페 공인 IP 확인: `183.99.218.162` (집 IP는 나중에 `terraform.tfvars`에 추가)
- `infra/` 디렉토리 생성 완료
- `.gitignore`에 Terraform 항목 추가 완료

### 완료된 Terraform 파일 작성

| 파일 | 상태 |
|------|------|
| `infra/provider.tf` | ✅ 완료 |
| `infra/variables.tf` | ✅ 완료 |
| `infra/main.tf` | ✅ 완료 |
| `infra/iam.tf` | ✅ 완료 |
| `infra/outputs.tf` | ✅ 완료 |
| `infra/userdata.sh` | ⬜ 미완료 |
| `infra/terraform.tfvars` | ⬜ 미완료 |

### 다음 할 일 (이어서 시작할 것)
1. `infra/userdata.sh` 작성 (Docker 설치 + `.env` 생성 스크립트)
2. `infra/terraform.tfvars` 작성 (실제 변수 값 입력 — `my_ips`에 카페 IP + 집 IP 포함)
3. `terraform init → plan → apply`로 인프라 생성
4. 인프라 검증 (SSH 접속, Docker 확인, SSM 연결 확인)
5. GitHub Secrets 추가 (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `EC2_INSTANCE_ID`)
6. `deploy.yml` deploy Job 수정 (SSM Send-Command 방식)
7. main push → 전체 파이프라인 테스트
8. NCP runner 해제 및 NCP 인스턴스 반납

## 참고: 코드 작성 규칙
- Claude는 `.md` 파일 외에는 직접 생성/수정하지 않음
- 코드 스니펫을 보여주면 사용자가 직접 타이핑하며 작성
