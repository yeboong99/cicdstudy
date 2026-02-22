# PL Agent Webhook Contract

`PL Auto Merge` 워크플로우는 PR CI 완료 후 PL 에이전트 웹훅을 호출해 merge 허용 여부를 판단한다.

## GitHub Secrets
- `PL_AGENT_WEBHOOK_URL` (필수)
- `PL_AGENT_API_KEY` (선택, Bearer 토큰으로 전달)

## Request
- Method: `POST`
- Header: `Content-Type: application/json`
- Header: `Authorization: Bearer <PL_AGENT_API_KEY>` (선택)

요청 바디 주요 필드:
- `event`: `pl_merge_decision`
- `repository`: `owner/repo`
- `pr_number`: number
- `head_branch`, `head_sha`
- `ci_conclusion`
- `pr`: `gh pr view --json ...` 기반 메타데이터
- `diff`: PR diff 문자열(최대 120000자)

## Response (required)
```json
{
  "decision": "approve",
  "summary": "테스트/변경 영향 검토 결과 머지 가능",
  "comment": "리스크 없음. 머지 후 배포 파이프라인 확인 필요",
  "merge_method": "merge"
}
```

### `decision` 허용값
- `approve` (`approved`, `allow`, `merge`도 허용)
- `reject` (`deny`, `denied`도 허용)
- `needs_human` (`manual`, `hold`도 허용)

### `merge_method`
- `merge`
- `squash`
- `rebase`

## Runtime Behavior
- `approve`: 자동 approve + auto-merge 설정
- `reject`: PR 코멘트로 반려 사유 게시
- `needs_human`: PR 코멘트로 수동 판단 요청
- 웹훅 실패/응답 파싱 실패: 워크플로우 실패 + PR 코멘트 게시
