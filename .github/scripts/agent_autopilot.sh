#!/usr/bin/env bash
set -Eeuo pipefail

BASE_BRANCH="${BASE_BRANCH:-main}"
BRANCH_PREFIX_REQUIRED="${BRANCH_PREFIX_REQUIRED:-yeboong99/}"
DOCKER_COMPOSE_FILE="${DOCKER_COMPOSE_FILE:-docker-compose.yml}"
SMOKE_TEST_URL="${SMOKE_TEST_URL:-http://localhost:8080/cal/add?firstNumber=1&secondNumber=2}"
SMOKE_EXPECTED_TEXT="${SMOKE_EXPECTED_TEXT:-\"result\":\"3\"}"
MAX_FIX_ATTEMPTS="${MAX_FIX_ATTEMPTS:-2}"
AUTO_FIX_COMMAND="${AUTO_FIX_COMMAND:-}"
COMMIT_PREFIX="${COMMIT_PREFIX:-[Auto]}"
PR_TITLE_PREFIX="${PR_TITLE_PREFIX:-[Auto]}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-forcicd_local}"
DRY_RUN="${DRY_RUN:-false}"

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "Required command not found: ${cmd}"
    exit 1
  fi
}

run_cmd() {
  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[DRY_RUN] $*"
    return 0
  fi
  "$@"
}

current_branch() {
  git rev-parse --abbrev-ref HEAD
}

working_tree_has_changes() {
  if ! git diff --quiet; then
    return 0
  fi
  if ! git diff --cached --quiet; then
    return 0
  fi
  if [[ -n "$(git ls-files --others --exclude-standard)" ]]; then
    return 0
  fi
  return 1
}

cleanup_docker() {
  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[DRY_RUN] docker compose -f ${DOCKER_COMPOSE_FILE} down --remove-orphans -v"
    return 0
  fi

  DB_USERNAME="${DB_USERNAME}" DB_PASSWORD="${DB_PASSWORD}" \
    docker compose -f "${DOCKER_COMPOSE_FILE}" down --remove-orphans -v >/dev/null 2>&1 || true
}

wait_for_smoke() {
  local attempt
  local response

  for attempt in $(seq 1 60); do
    response="$(curl -fsS "${SMOKE_TEST_URL}" 2>/dev/null || true)"
    if [[ -n "${response}" ]] && [[ "${response}" == *"${SMOKE_EXPECTED_TEXT}"* ]]; then
      echo "Smoke test succeeded on attempt ${attempt}."
      return 0
    fi
    sleep 2
  done

  echo "Smoke test failed: ${SMOKE_TEST_URL} did not return expected text '${SMOKE_EXPECTED_TEXT}'."
  return 1
}

run_verification() {
  echo "Running docker-based smoke test and Gradle tests..."

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[DRY_RUN] docker compose -f ${DOCKER_COMPOSE_FILE} up -d --build"
    echo "[DRY_RUN] curl ${SMOKE_TEST_URL}"
    echo "[DRY_RUN] ./gradlew clean test"
    return 0
  fi

  cleanup_docker

  DB_USERNAME="${DB_USERNAME}" DB_PASSWORD="${DB_PASSWORD}" \
    docker compose -f "${DOCKER_COMPOSE_FILE}" up -d --build

  if ! wait_for_smoke; then
    DB_USERNAME="${DB_USERNAME}" DB_PASSWORD="${DB_PASSWORD}" \
      docker compose -f "${DOCKER_COMPOSE_FILE}" logs app --tail=120 || true
    return 1
  fi

  ./gradlew clean test
}

summarize_changes() {
  git diff --cached --name-only | awk -F/ '{print $1}' | sort -u | paste -sd, -
}

list_changed_files_against_base() {
  git fetch origin "${BASE_BRANCH}" >/dev/null 2>&1 || true
  git diff --name-only "origin/${BASE_BRANCH}...HEAD"
}

generate_commit_message() {
  local branch="$1"
  local file_count
  local scopes

  file_count="$(git diff --cached --name-only | wc -l | tr -d ' ')"
  scopes="$(summarize_changes)"

  if [[ -z "${scopes}" ]]; then
    scopes="misc"
  fi

  echo "${COMMIT_PREFIX} ${branch#*/}: update ${file_count} files (${scopes})"
}

generate_pr_title() {
  local branch="$1"
  echo "${PR_TITLE_PREFIX} ${branch#*/}"
}

generate_pr_body() {
  local branch="$1"
  local body_file="$2"
  local changed_files

  changed_files="$(list_changed_files_against_base)"

  {
    echo "## 작업 브랜치"
    echo
    echo "- 브랜치명: \`${branch}\`"
    echo
    echo "## 변경 내용"
    echo
    if [[ -n "${changed_files}" ]]; then
      echo "${changed_files}" | sed 's/^/- /'
    else
      echo "- 변경 파일 목록을 찾지 못했습니다."
    fi
    echo
    echo "## 리뷰 요청 사항"
    echo
    echo "- 자동 생성 PR입니다. Docker 스모크 테스트와 \`./gradlew clean test\`를 통과했습니다."
    echo "- 자동수정 훅은 \`AUTO_FIX_COMMAND\` 환경변수로 지정할 수 있습니다."
  } > "${body_file}"
}

main() {
  require_cmd git
  require_cmd gh
  require_cmd docker
  require_cmd curl

  local branch
  local attempt
  local pr_number
  local commit_message
  local pr_title
  local pr_body_file

  branch="$(current_branch)"

  if [[ "${branch}" == "HEAD" ]]; then
    echo "Detached HEAD is not supported."
    exit 1
  fi

  if [[ "${branch}" == "${BASE_BRANCH}" ]]; then
    echo "Current branch is base branch (${BASE_BRANCH}). Use a feature branch."
    exit 1
  fi

  if [[ -n "${BRANCH_PREFIX_REQUIRED}" ]] && [[ "${branch}" != ${BRANCH_PREFIX_REQUIRED}* ]]; then
    echo "Branch '${branch}' does not match required prefix '${BRANCH_PREFIX_REQUIRED}'."
    exit 1
  fi

  trap cleanup_docker EXIT

  for attempt in $(seq 1 "${MAX_FIX_ATTEMPTS}"); do
    if run_verification; then
      echo "Verification passed."
      break
    fi

    if [[ "${attempt}" -eq "${MAX_FIX_ATTEMPTS}" ]]; then
      echo "Verification failed after ${MAX_FIX_ATTEMPTS} attempts."
      exit 1
    fi

    if [[ -z "${AUTO_FIX_COMMAND}" ]]; then
      echo "Verification failed and AUTO_FIX_COMMAND is empty."
      exit 1
    fi

    echo "Verification failed. Running AUTO_FIX_COMMAND (attempt ${attempt}/${MAX_FIX_ATTEMPTS})..."
    if [[ "${DRY_RUN}" == "true" ]]; then
      echo "[DRY_RUN] ${AUTO_FIX_COMMAND}"
    else
      bash -lc "${AUTO_FIX_COMMAND}"
    fi
  done

  if working_tree_has_changes; then
    run_cmd git add -A
    commit_message="$(generate_commit_message "${branch}")"
    run_cmd git commit -m "${commit_message}"
  else
    echo "No local file changes detected. Skip commit step."
  fi

  run_cmd git push -u origin "${branch}"

  pr_number="$(gh pr list --head "${branch}" --state open --json number --jq '.[0].number // empty')"
  if [[ -n "${pr_number}" ]]; then
    echo "PR #${pr_number} already exists for branch ${branch}."
    exit 0
  fi

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[DRY_RUN] Skip PR create"
    exit 0
  fi

  pr_title="$(generate_pr_title "${branch}")"
  pr_body_file="$(mktemp)"
  generate_pr_body "${branch}" "${pr_body_file}"

  gh pr create \
    --base "${BASE_BRANCH}" \
    --head "${branch}" \
    --title "${pr_title}" \
    --body-file "${pr_body_file}"

  rm -f "${pr_body_file}"
  echo "Autopilot completed: tests passed, commit/push/PR done."
}

main "$@"
