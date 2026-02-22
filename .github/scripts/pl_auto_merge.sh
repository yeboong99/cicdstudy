#!/usr/bin/env bash
set -Eeuo pipefail

REPO="${REPO:?REPO is required}"
HEAD_BRANCH="${HEAD_BRANCH:-}"
HEAD_SHA="${HEAD_SHA:-}"
CI_CONCLUSION="${CI_CONCLUSION:-}"
PR_NUMBER_INPUT="${PR_NUMBER:-}"
POLICY_FILE=".github/pl-merge-policy.conf"
PL_AGENT_WEBHOOK_URL="${PL_AGENT_WEBHOOK_URL:-}"
PL_AGENT_API_KEY="${PL_AGENT_API_KEY:-}"

if [[ ! -f "${POLICY_FILE}" ]]; then
  echo "Policy file not found: ${POLICY_FILE}"
  exit 1
fi

# shellcheck disable=SC1090
source "${POLICY_FILE}"

BASE_BRANCH="${BASE_BRANCH:-main}"
ALLOWED_HEAD_PREFIXES="${ALLOWED_HEAD_PREFIXES:-}"
DEFAULT_MERGE_METHOD="${DEFAULT_MERGE_METHOD:-merge}"
PL_AGENT_TIMEOUT_SECONDS="${PL_AGENT_TIMEOUT_SECONDS:-45}"

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

merge_flag_from_method() {
  local method="${1:-merge}"
  case "${method}" in
    squash)
      echo "--squash"
      ;;
    rebase)
      echo "--rebase"
      ;;
    merge|*)
      echo "--merge"
      ;;
  esac
}

resolve_pr_number() {
  if [[ -n "${PR_NUMBER_INPUT}" ]]; then
    echo "${PR_NUMBER_INPUT}"
    return 0
  fi

  if [[ -n "${HEAD_SHA}" ]]; then
    local by_sha
    by_sha="$(gh api \
      -H "Accept: application/vnd.github+json" \
      "/repos/${REPO}/commits/${HEAD_SHA}/pulls" \
      --jq 'map(select(.state == "open")) | .[0].number // empty')"
    if [[ -n "${by_sha}" ]]; then
      echo "${by_sha}"
      return 0
    fi
  fi

  if [[ -n "${HEAD_BRANCH}" ]]; then
    gh pr list \
      --repo "${REPO}" \
      --state open \
      --head "${HEAD_BRANCH}" \
      --json number \
      --jq '.[0].number // empty'
    return 0
  fi

  echo ""
}

comment_pr() {
  local pr_number="$1"
  local body="$2"
  gh pr comment "${pr_number}" --repo "${REPO}" --body "${body}" >/dev/null
}

PR_NUMBER="$(resolve_pr_number)"
if [[ -z "${PR_NUMBER}" ]]; then
  echo "No open PR found for head=${HEAD_BRANCH}, sha=${HEAD_SHA}"
  exit 0
fi

echo "Target PR: #${PR_NUMBER}"

PR_CONTEXT_JSON="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json number,title,body,url,baseRefName,headRefName,isDraft,isCrossRepository,mergeStateStatus,reviewDecision,author,files,changedFiles,additions,deletions,statusCheckRollup)"
PR_BASE="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json baseRefName --jq '.baseRefName')"
PR_HEAD="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json headRefName --jq '.headRefName')"
PR_DRAFT="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json isDraft --jq '.isDraft')"
PR_CROSS_REPO="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json isCrossRepository --jq '.isCrossRepository')"
PR_MERGE_STATE="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json mergeStateStatus --jq '.mergeStateStatus')"

if [[ "${PR_DRAFT}" == "true" ]]; then
  echo "PR is draft. Skip."
  exit 0
fi

if [[ "${PR_BASE}" != "${BASE_BRANCH}" ]]; then
  echo "PR base is ${PR_BASE}, expected ${BASE_BRANCH}. Skip."
  exit 0
fi

if [[ "${PR_CROSS_REPO}" == "true" ]]; then
  comment_pr "${PR_NUMBER}" "PL 자동 머지 중단: 외부 포크 PR은 PL 에이전트 자동 머지 대상이 아닙니다."
  exit 0
fi

if [[ -n "${ALLOWED_HEAD_PREFIXES}" ]]; then
  IFS=',' read -r -a PREFIXES <<< "${ALLOWED_HEAD_PREFIXES}"
  ALLOWED_BRANCH=false
  for raw_prefix in "${PREFIXES[@]}"; do
    prefix="$(trim "${raw_prefix}")"
    [[ -z "${prefix}" ]] && continue
    if [[ "${PR_HEAD}" == "${prefix}"* ]]; then
      ALLOWED_BRANCH=true
      break
    fi
  done
  if [[ "${ALLOWED_BRANCH}" != "true" ]]; then
    comment_pr "${PR_NUMBER}" "PL 자동 머지 중단: head 브랜치(\`${PR_HEAD}\`)가 허용 prefix 조건을 만족하지 않습니다."
    exit 0
  fi
fi

if [[ -n "${CI_CONCLUSION}" && "${CI_CONCLUSION}" != "success" ]]; then
  comment_pr "${PR_NUMBER}" "PL 자동 머지 대기: PR CI 결과가 success가 아닙니다. (현재: ${CI_CONCLUSION})"
  exit 0
fi

if [[ "${PR_MERGE_STATE}" == "DIRTY" ]]; then
  comment_pr "${PR_NUMBER}" "PL 자동 머지 중단: 현재 충돌 상태(DIRTY)입니다. 충돌 해결 후 재평가합니다."
  exit 0
fi

if [[ -z "${PL_AGENT_WEBHOOK_URL}" ]]; then
  comment_pr "${PR_NUMBER}" "PL 자동 머지 실패: \`PL_AGENT_WEBHOOK_URL\` 시크릿이 설정되지 않아 PL 에이전트 판단을 요청할 수 없습니다."
  exit 1
fi

PR_DIFF="$(gh pr diff "${PR_NUMBER}" --repo "${REPO}" || true)"
PAYLOAD_FILE="$(mktemp)"
RESPONSE_FILE="$(mktemp)"
RESULT_ENV_FILE="$(mktemp)"

trap 'rm -f "${PAYLOAD_FILE}" "${RESPONSE_FILE}" "${RESULT_ENV_FILE}"' EXIT

export PR_CONTEXT_JSON
export PR_DIFF
export CI_CONCLUSION
export REPO
export PR_NUMBER
export HEAD_BRANCH
export HEAD_SHA

python3 - <<'PY' > "${PAYLOAD_FILE}"
import json
import os

pr_context = json.loads(os.environ.get("PR_CONTEXT_JSON", "{}"))
pr_diff = os.environ.get("PR_DIFF", "")

payload = {
    "event": "pl_merge_decision",
    "repository": os.environ.get("REPO", ""),
    "pr_number": int(os.environ.get("PR_NUMBER", "0") or 0),
    "head_branch": os.environ.get("HEAD_BRANCH", ""),
    "head_sha": os.environ.get("HEAD_SHA", ""),
    "ci_conclusion": os.environ.get("CI_CONCLUSION", ""),
    "pr": pr_context,
    "diff": pr_diff[:120000],
    "instructions": {
        "required_output": {
            "decision": "approve | reject | needs_human",
            "summary": "one-line rationale",
            "comment": "markdown review comment",
            "merge_method": "merge | squash | rebase"
        }
    }
}

print(json.dumps(payload, ensure_ascii=False))
PY

CURL_ARGS=(
  -fsS
  -X POST
  "${PL_AGENT_WEBHOOK_URL}"
  -H "Content-Type: application/json"
  --max-time "${PL_AGENT_TIMEOUT_SECONDS}"
  --data-binary "@${PAYLOAD_FILE}"
)

if [[ -n "${PL_AGENT_API_KEY}" ]]; then
  CURL_ARGS+=( -H "Authorization: Bearer ${PL_AGENT_API_KEY}" )
fi

set +e
curl "${CURL_ARGS[@]}" > "${RESPONSE_FILE}"
CURL_EXIT=$?
set -e

if [[ ${CURL_EXIT} -ne 0 ]]; then
  comment_pr "${PR_NUMBER}" "PL 자동 머지 실패: PL 에이전트 호출에 실패했습니다 (exit=${CURL_EXIT})."
  exit 1
fi

python3 - "${RESPONSE_FILE}" <<'PY' > "${RESULT_ENV_FILE}"
import json
import shlex
import sys

path = sys.argv[1]
with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)

decision = str(data.get("decision", "")).strip().lower()
summary = str(data.get("summary", "")).strip()
comment = str(data.get("comment", "")).strip()
merge_method = str(data.get("merge_method", "")).strip().lower()

print(f"DECISION={shlex.quote(decision)}")
print(f"SUMMARY={shlex.quote(summary)}")
print(f"COMMENT={shlex.quote(comment)}")
print(f"MERGE_METHOD={shlex.quote(merge_method)}")
PY

# shellcheck disable=SC1090
source "${RESULT_ENV_FILE}"

AGENT_SUMMARY="${SUMMARY:-PL 에이전트 요약이 제공되지 않았습니다.}"
AGENT_COMMENT="${COMMENT:-}"
AGENT_DECISION="${DECISION:-}"
AGENT_MERGE_METHOD="${MERGE_METHOD:-${DEFAULT_MERGE_METHOD}}"

if [[ -z "${AGENT_DECISION}" ]]; then
  comment_pr "${PR_NUMBER}" "PL 자동 머지 실패: PL 에이전트 응답에 decision 필드가 없습니다."
  exit 1
fi

case "${AGENT_DECISION}" in
  approve|approved|allow|merge)
    REVIEW_BODY="PL 에이전트 승인: ${AGENT_SUMMARY}"
    if [[ -n "${AGENT_COMMENT}" ]]; then
      REVIEW_BODY+=$'\n\n'
      REVIEW_BODY+="${AGENT_COMMENT}"
    fi

    gh pr review "${PR_NUMBER}" --repo "${REPO}" --approve --body "${REVIEW_BODY}" || true

    MERGE_FLAG="$(merge_flag_from_method "${AGENT_MERGE_METHOD}")"
    gh pr merge "${PR_NUMBER}" --repo "${REPO}" --auto "${MERGE_FLAG}"
    echo "PL agent approved PR #${PR_NUMBER}, auto-merge enabled (${MERGE_FLAG})."
    ;;
  reject|denied|deny)
    COMMENT_BODY="PL 에이전트 반려: ${AGENT_SUMMARY}"
    if [[ -n "${AGENT_COMMENT}" ]]; then
      COMMENT_BODY+=$'\n\n'
      COMMENT_BODY+="${AGENT_COMMENT}"
    fi
    comment_pr "${PR_NUMBER}" "${COMMENT_BODY}"
    echo "PL agent rejected PR #${PR_NUMBER}."
    ;;
  needs_human|manual|hold)
    COMMENT_BODY="PL 에이전트 수동판단 요청: ${AGENT_SUMMARY}"
    if [[ -n "${AGENT_COMMENT}" ]]; then
      COMMENT_BODY+=$'\n\n'
      COMMENT_BODY+="${AGENT_COMMENT}"
    fi
    comment_pr "${PR_NUMBER}" "${COMMENT_BODY}"
    echo "PL agent requested manual decision for PR #${PR_NUMBER}."
    ;;
  *)
    comment_pr "${PR_NUMBER}" "PL 자동 머지 실패: 알 수 없는 decision(\`${AGENT_DECISION}\`)이 반환되었습니다."
    exit 1
    ;;
esac
