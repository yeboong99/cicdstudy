# LEARNINGS.md
> 이 문서는 forcicd 프로젝트를 통해 진행한 내용, 배운 개념, 심층 대화 내용을 정리한 기록입니다.

---

## 1. 프로젝트 전체 흐름 요약

### Phase 1 — CI/CD 파이프라인 구축 (NCP)
- Spring Boot 앱에 GitHub Actions 기반 CI/CD 파이프라인 적용
- 4개 Job 구성: `build` → `tag` → `docker` → `deploy`
- NCP 서버에 self-hosted runner(`work-1`) 설치 후 Docker Compose 배포 방식 채택
- GHCR(GitHub Container Registry)에 이미지 push 후 서버에서 pull

### Phase 2 — 배포 안정화
- `docker-compose.prod.yml` 생성 (로컬용과 분리)
- `restart: unless-stopped` 정책으로 서버 재부팅 후 자동 재시작
- compose 파일을 `~/app/`에 고정 복사 → 수동 관리 가능하도록 개선
- 환경변수를 서버의 `~/app/.env`에서 직접 관리하는 방식으로 변경

### Phase 3 — AWS 전환 + Terraform 도입
- NCP → AWS EC2로 인프라 전환
- **Terraform**으로 인프라 전체를 코드로 관리 (IaC)
- self-hosted runner 제거 → **AWS SSM Send-Command** 방식으로 배포 전환
- `terraform apply` 한 번으로 아래 12개 리소스 자동 생성:
  - VPC, 퍼블릭 서브넷, 인터넷 게이트웨이, 라우팅 테이블
  - 보안 그룹 (SSH: 허용 IP만, 8080: 전체)
  - EC2 (Ubuntu 22.04, t3.small, 20GB gp3)
  - Elastic IP (고정 IP)
  - IAM 역할 + SSM 정책
- `terraform destroy`로 전체 리소스 일괄 삭제

---

## 2. Terraform 파일 구조

| 파일 | 역할 |
|------|------|
| `provider.tf` | AWS 프로바이더 설정 |
| `variables.tf` | 변수 스키마 선언 (타입, 기본값) |
| `terraform.tfvars` | 실제 변수 값 (gitignore 대상) |
| `main.tf` | 인프라 리소스 정의 |
| `iam.tf` | IAM 역할 + SSM 정책 |
| `outputs.tf` | 결과값 출력 (IP, 인스턴스 ID 등) |
| `userdata.sh` | EC2 초기화 스크립트 (Docker 설치, .env 생성) |

### Terraform 핵심 명령어
```bash
terraform init     # 프로바이더 플러그인 다운로드
terraform plan     # 변경사항 미리보기 (실제 적용 안 함)
terraform apply    # 인프라 생성/변경
terraform destroy  # 모든 리소스 삭제
```

---

## 3. 배포 방식 변천사

```
[Phase 1-2]
GitHub Actions → self-hosted runner (EC2 상주 프로세스) → docker compose 실행

[Phase 3]
GitHub Actions → AWS SSM Send-Command → EC2가 직접 docker compose 실행
```

### SSM 방식의 장점
- EC2에 runner 프로세스 상주 불필요 → 메모리 절약 (~200MB)
- runner 버전 업데이트, 토큰 재발급 등 유지보수 불필요
- EC2 재생성 시 runner 재등록 불필요
- SSM Agent는 Ubuntu AMI에 기본 포함

---

## 4. 트러블슈팅 기록

### 테스트 DB 연결 오류
- **원인**: GitHub runner에 MySQL 없음 → `contextLoads()` 테스트 실패
- **해결**: `testRuntimeOnly 'com.h2database:h2'` 추가 + 테스트용 H2 인메모리 DB 설정

### MySQL 사용자 미존재
- **원인**: `docker-compose.prod.yml`에 `MYSQL_USER`, `MYSQL_PASSWORD` 없음 → root만 생성됨
- **해결**: `MYSQL_USER: ${DB_USERNAME}`, `MYSQL_PASSWORD: ${DB_PASSWORD}` 추가
- **주의**: 볼륨이 이미 존재하면 재초기화 안 됨 → `docker compose down -v` 후 재시작 필요

### SSM 배포 실패 — curl 404
- **원인**: private 레포의 `docker-compose.prod.yml`을 raw.githubusercontent.com으로 다운로드 시도 → 인증 불가
- **해결**: GitHub runner에서 파일을 base64로 인코딩 후 SSM 명령에 직접 포함

### compose 파일 경로 소실
- **원인**: deploy job이 checkout 디렉토리에서 실행 → job 종료 후 파일 소실
- **해결**: `~/app/`에 파일을 복사 후 실행

---

## 5. 심층 학습: 보안 개념

### 대칭키 vs 비대칭키(공개키) 암호화

**대칭키**: 암호화/복호화에 같은 키 사용. 키 전달 과정에서 탈취 위험.

**비대칭키(공개키)**:
- 개인키(private key): 나만 보유, 절대 공개 안 함
- 공개키(public key): 누구에게나 공개 가능
- 한 키로 암호화하면 반드시 짝 키로만 복호화 가능

**두 가지 활용 방식:**
1. **암호화 통신** (기밀성): 상대방 공개키로 암호화 → 상대방 개인키로만 복호화
2. **전자서명** (무결성): 개인키로 서명 → 공개키로 검증

---

### GPG 서명 검증 원리

Docker 패키지 설치 시 서명 검증 흐름:

```
[Docker 배포 시]
패키지 내용 → 해시 함수 → 해시값
해시값 → Docker 개인키로 암호화 → 서명값
패키지 + 서명값 함께 배포

[우리가 설치 시]
① 받은 패키지 → 해시 함수 → 해시값A  (직접 계산)
② 서명값 → Docker 공개키로 복호화 → 해시값B  (Docker가 만든 것)

해시값A == 해시값B → 진짜 Docker 패키지
```

> 핵심: 서명값을 공개키로 복호화했을 때 나오는 해시값과, 내가 직접 계산한 해시값이 일치해야 함.

---

### SSH 키 인증 원리

```
1. 내가 EC2에 SSH 접속 시도
2. EC2가 랜덤 챌린지값 생성 → 나에게 전송
3. 나는 챌린지값을 개인키로 서명 → EC2에 전송
4. EC2는 저장된 공개키로 서명값 복호화
5. 복호화된 값 == 원래 챌린지값 → 접속 허용
```

> 비밀번호는 "맞는 값을 알고 있냐"이지만,
> SSH 키는 "개인키를 실제로 갖고 있냐"를 증명하는 방식.
> 개인키는 네트워크로 전송되지 않아 더 안전.

---

## 6. 심층 학습: Linux / Docker 개념

### shebang (`#!/bin/bash`)
- 스크립트를 어떤 프로그램으로 실행할지 OS에 알려주는 선언
- `#` (sharp/hash) + `!` (bang) = shebang
- 없으면 기본 shell(`sh`)로 실행 → bash 전용 문법 사용 시 오류 가능

### bash vs sh vs zsh
- **shell**: 명령어를 OS에 전달하는 프로그램 (터미널 환경)
- **sh**: 가장 기본적인 shell
- **bash** (Bourne Again Shell): 리눅스 서버의 기본 shell
- **zsh**: bash의 업그레이드 버전. macOS 기본 shell

### `set -e`
- 스크립트 실행 중 명령어 하나라도 실패하면 즉시 중단
- 초기화 스크립트에서 안전을 위해 관례적으로 사용

### apt vs apt-get
- `apt`: 사용자 친화적 (진행 바, 색상 등). 터미널에서 직접 사용
- `apt-get`: 스크립트용. 대화형 출력 없어서 로그가 깔끔

### apt 구조
```
apt 소스 목록 (/etc/apt/sources.list.d/)
├── ubuntu.list    ← Ubuntu 공식 저장소
├── docker.list    ← Docker 공식 저장소 (직접 추가)

apt-get update     → 저장소에서 패키지 목록 갱신 (설치 X)
apt-get install    → 목록에서 찾아서 다운로드 + 설치
```

### CPU 아키텍처
- `amd64`: 일반 PC/서버 (Intel/AMD). EC2 t3.small이 이것
- `arm64`: 애플 M1/M2, 스마트폰
- 같은 프로그램도 아키텍처마다 다른 바이너리가 필요

### heredoc (`<< 'EOF' ... EOF`)
- bash에서 여러 줄 문자열을 입력으로 전달하는 문법
- `'EOF'` (작은따옴표): 변수 치환 없이 문자열 그대로 전달
- `"EOF"` (큰따옴표): 변수 치환 발생

### `$()` vs `${}`
- `$()`: 괄호 안 명령어를 실행하고 결과를 반환 (command substitution)
- `${}`: 변수 값을 참조 (variable expansion)

---

## 7. 핵심 교훈

1. **IaC(Infrastructure as Code)의 가치**: `terraform apply` 한 번으로 인프라 생성, `terraform destroy` 한 번으로 완전 삭제. 재현 가능하고 버전 관리 가능.

2. **self-hosted runner의 단점**: 항상 프로세스가 상주해야 하고, 버전 관리/재등록 등 유지보수 비용이 발생. SSM으로 대체하면 서버 부담 감소.

3. **MySQL 초기화는 볼륨이 비어있을 때만**: `MYSQL_USER` 등 환경변수로 사용자 자동 생성은 최초 1회만. 볼륨이 있으면 `down -v`로 삭제 후 재시작 필요.

4. **민감한 정보는 절대 코드에**: `terraform.tfvars`는 gitignore, `.env`는 서버에서 직접 관리.

5. **보안의 기본은 비대칭키**: SSH, GPG, HTTPS 등 현대 보안의 대부분이 비대칭키 기반. 원리를 이해하면 모든 보안 도구가 연결됨.
