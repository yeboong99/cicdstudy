#!/usr/bin/env bash
set -Eeuo pipefail

REPO="${REPO:?REPO is required}"
EVENT_NAME="${EVENT_NAME:-}"
HEAD_BRANCH="${HEAD_BRANCH:-}"
HEAD_SHA="${HEAD_SHA:-}"
CI_CONCLUSION="${CI_CONCLUSION:-}"
PR_NUMBER_INPUT="${PR_NUMBER:-}"
POLICY_FILE=".github/pl-merge-policy.conf"

if [[ ! -f "${POLICY_FILE}" ]]; then
  echo "Policy file not found: ${POLICY_FILE}"
  exit 1
fi

# shellcheck disable=SC1090
source "${POLICY_FILE}"

BASE_BRANCH="${BASE_BRANCH:-main}"
MERGE_METHOD="${MERGE_METHOD:-merge}"
ALLOWED_HEAD_PREFIXES="${ALLOWED_HEAD_PREFIXES:-}"
BLOCKED_PATH_PATTERNS="${BLOCKED_PATH_PATTERNS:-}"
APPROVAL_MESSAGE="${APPROVAL_MESSAGE:-PL auto review passed.}"
MANUAL_REQUIRED_MESSAGE="${MANUAL_REQUIRED_MESSAGE:-Manual review required.}"

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

if [[ -n "${PR_NUMBER_INPUT}" ]]; then
  PR_NUMBER="${PR_NUMBER_INPUT}"
else
  if [[ -n "${HEAD_SHA}" ]]; then
    PR_NUMBER="$(gh api \
      -H "Accept: application/vnd.github+json" \
      "/repos/${REPO}/commits/${HEAD_SHA}/pulls" \
      --jq 'map(select(.state == "open")) | .[0].number // empty')"
  fi

  if [[ -z "${PR_NUMBER:-}" ]]; then
    if [[ -z "${HEAD_BRANCH}" ]]; then
      echo "No HEAD_BRANCH/HEAD_SHA provided and no PR_NUMBER provided. Nothing to do."
      exit 0
    fi

    PR_NUMBER="$(gh pr list \
      --repo "${REPO}" \
      --state open \
      --head "${HEAD_BRANCH}" \
      --json number \
      --jq '.[0].number // empty')"
  fi

  if [[ -z "${PR_NUMBER}" ]]; then
    echo "No open PR found for head=${HEAD_BRANCH}, sha=${HEAD_SHA}"
    exit 0
  fi
fi

echo "Target PR: #${PR_NUMBER}"

PR_BASE="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json baseRefName --jq '.baseRefName')"
PR_HEAD="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json headRefName --jq '.headRefName')"
PR_DRAFT="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json isDraft --jq '.isDraft')"
PR_CROSS_REPO="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json isCrossRepository --jq '.isCrossRepository')"
PR_MERGE_STATE="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json mergeStateStatus --jq '.mergeStateStatus')"
PR_REVIEW_DECISION="$(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json reviewDecision --jq '.reviewDecision // ""')"

if [[ "${PR_DRAFT}" == "true" ]]; then
  echo "PR is draft. Skip."
  exit 0
fi

if [[ "${PR_BASE}" != "${BASE_BRANCH}" ]]; then
  echo "PR base is ${PR_BASE}, policy base is ${BASE_BRANCH}. Skip."
  exit 0
fi

if [[ "${PR_CROSS_REPO}" == "true" ]]; then
  echo "Cross-repository PR detected. Skip auto-merge for safety."
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
    echo "PR head branch '${PR_HEAD}' does not match allowed prefixes. Skip."
    exit 0
  fi
fi

if [[ -n "${CI_CONCLUSION}" && "${CI_CONCLUSION}" != "success" ]]; then
  echo "CI conclusion is ${CI_CONCLUSION}. Skip auto-merge."
  exit 0
fi

if [[ "${PR_REVIEW_DECISION}" == "CHANGES_REQUESTED" ]]; then
  echo "Review decision is CHANGES_REQUESTED. Skip."
  exit 0
fi

if [[ "${PR_MERGE_STATE}" == "DIRTY" ]]; then
  gh pr comment "${PR_NUMBER}" --repo "${REPO}" --body "PL 자동 리뷰 결과: 현재 충돌 상태(DIRTY)라 자동 머지를 중단했습니다. rebase/merge 후 재시도됩니다."
  exit 0
fi

mapfile -t CHANGED_FILES < <(gh pr view "${PR_NUMBER}" --repo "${REPO}" --json files --jq '.files[].path')

IFS=',' read -r -a PATTERNS <<< "${BLOCKED_PATH_PATTERNS}"
BLOCKED_FILES=()

for file in "${CHANGED_FILES[@]}"; do
  for raw_pattern in "${PATTERNS[@]}"; do
    pattern="$(trim "${raw_pattern}")"
    [[ -z "${pattern}" ]] && continue
    if [[ "${file}" == ${pattern} ]]; then
      BLOCKED_FILES+=("${file}")
      break
    fi
  done
done

if (( ${#BLOCKED_FILES[@]} > 0 )); then
  {
    echo "${MANUAL_REQUIRED_MESSAGE}"
    echo
    echo "차단된 변경 파일:"
    for file in "${BLOCKED_FILES[@]}"; do
      echo "- \`${file}\`"
    done
  } > /tmp/pl_manual_required_comment.md

  gh pr comment "${PR_NUMBER}" --repo "${REPO}" --body-file /tmp/pl_manual_required_comment.md
  echo "Manual review required due to blocked file patterns."
  exit 0
fi

gh pr review "${PR_NUMBER}" --repo "${REPO}" --approve --body "${APPROVAL_MESSAGE}" || true

MERGE_FLAG="--merge"
case "${MERGE_METHOD}" in
  squash)
    MERGE_FLAG="--squash"
    ;;
  rebase)
    MERGE_FLAG="--rebase"
    ;;
  merge)
    MERGE_FLAG="--merge"
    ;;
  *)
    echo "Unknown MERGE_METHOD=${MERGE_METHOD}, fallback to merge"
    MERGE_FLAG="--merge"
    ;;
esac

set +e
gh pr merge "${PR_NUMBER}" --repo "${REPO}" --auto "${MERGE_FLAG}"
MERGE_EXIT=$?
set -e

if [[ ${MERGE_EXIT} -ne 0 ]]; then
  echo "Failed to enable auto-merge (exit=${MERGE_EXIT})."
  exit ${MERGE_EXIT}
fi

echo "Auto-merge enabled for PR #${PR_NUMBER}"
