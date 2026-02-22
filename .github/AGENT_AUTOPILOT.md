# Agent Autopilot

개발 에이전트가 작업 종료 시 아래 한 줄로 자동 검증/커밋/PR 생성을 수행한다.

```bash
.github/scripts/agent_autopilot.sh
```

## 수행 순서
1. `docker compose -f docker-compose.yml up -d --build`
2. 스모크 테스트: `GET /cal/add?firstNumber=1&secondNumber=2`
3. `./gradlew clean test`
4. 실패 시 `AUTO_FIX_COMMAND` 실행 후 재시도 (`MAX_FIX_ATTEMPTS`)
5. 통과 시 자동 커밋/푸시
6. `main` 대상 PR 자동 생성 (기존 PR 있으면 생략)

## 주요 환경 변수
- `BASE_BRANCH` (기본: `main`)
- `BRANCH_PREFIX_REQUIRED` (기본: `yeboong99/`)
- `MAX_FIX_ATTEMPTS` (기본: `2`)
- `AUTO_FIX_COMMAND` (기본: 빈 값)
- `DB_USERNAME` (기본: `root`)
- `DB_PASSWORD` (기본: `forcicd_local`)
- `DRY_RUN` (기본: `false`)

## 자동수정 훅 예시
```bash
export AUTO_FIX_COMMAND='echo "fix hook" && ./gradlew test'
.github/scripts/agent_autopilot.sh
```

## 참고
- GitHub Actions 워크플로우 `.github/workflows/agent-branch-autopr.yml`는
  `yeboong99/**` 브랜치 push 시 Docker+Gradle 검증 후 PR이 없으면 자동 생성한다.
- 실제 코드 수정(버그 자동 수정)은 `AUTO_FIX_COMMAND`에 연결된 도구/스크립트가 담당한다.
