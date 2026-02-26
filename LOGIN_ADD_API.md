# LOGIN_ADD_API

## 1. Document Info
- Version: `v1.0`
- Date: `2026-02-26`
- Purpose: 회원 시스템(일반 로그인) + 계산 기록 저장/조회 기능의 공통 API 계약
- Rule: 프론트/백엔드 에이전트는 이 문서를 단일 기준으로 개발한다.

## 2. Scope
- 일반 로그인(소셜 로그인 없음)
- 로그인 사용자별 계산 기록 저장/조회
- 로그인 성공 후 계산 기록 자동 조회
- 기록 클릭 시 `firstNumber` 자동 입력을 위한 데이터 제공

## 3. Auth Model
- Auth 방식: 세션 쿠키(`JSESSIONID`)
- 로그인 성공 시 서버가 세션을 생성하고 쿠키를 반환
- 아래 API는 인증 필요:
  - `POST /api/v1/calculations`
  - `GET /api/v1/calculations/history`
  - `GET /api/v1/calculations/history/{historyId}`
  - `GET /api/v1/auth/me`
  - `POST /api/v1/auth/logout`

## 4. Common Rules
- Base path: `/api/v1`
- Content-Type: `application/json`
- Time format: ISO-8601 UTC (`2026-02-26T04:12:00Z`)
- Operator enum: `add | subtract | multiply | divide`

## 5. Common Error Response
```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "CALC_001",
  "message": "지원하지 않는 operator입니다: mod",
  "timestamp": "2026-02-26T04:12:00Z",
  "path": "/api/v1/calculations"
}
```

### Error Codes
- `AUTH_001`: 아이디 또는 비밀번호 불일치
- `AUTH_002`: 인증되지 않은 사용자
- `AUTH_003`: 이미 로그인된 세션
- `USER_001`: 이미 존재하는 username
- `USER_002`: 회원가입 요청값 검증 실패
- `CALC_001`: 지원하지 않는 operator
- `CALC_002`: 0으로 나눌 수 없음
- `CALC_003`: 숫자 파라미터/바디 검증 실패
- `CALC_404`: 계산 기록 없음(또는 본인 소유 아님)

---

## 6. Auth APIs

### 6.1 회원가입
`POST /api/v1/auth/signup`

Request:
```json
{
  "username": "alice01",
  "password": "P@ssword1234",
  "displayName": "Alice"
}
```

Validation:
- `username`: 4~20자, 영문/숫자/`_` 허용
- `password`: 8~64자
- `displayName`: 1~30자

Response `201 Created`:
```json
{
  "userId": 1,
  "username": "alice01",
  "displayName": "Alice",
  "createdAt": "2026-02-26T04:20:00Z"
}
```

Errors:
- `400 Bad Request` (`USER_002`)
- `409 Conflict` (`USER_001`)

### 6.2 로그인
`POST /api/v1/auth/login`

Request:
```json
{
  "username": "alice01",
  "password": "P@ssword1234"
}
```

Response `200 OK`:
```json
{
  "userId": 1,
  "username": "alice01",
  "displayName": "Alice",
  "sessionActive": true,
  "loggedInAt": "2026-02-26T04:25:00Z"
}
```

Errors:
- `400 Bad Request` (요청 검증 실패)
- `401 Unauthorized` (`AUTH_001`)

### 6.3 로그아웃
`POST /api/v1/auth/logout`

Response:
- `204 No Content`

Errors:
- `401 Unauthorized` (`AUTH_002`)

### 6.4 내 정보 조회
`GET /api/v1/auth/me`

Response `200 OK`:
```json
{
  "userId": 1,
  "username": "alice01",
  "displayName": "Alice",
  "createdAt": "2026-02-26T04:20:00Z"
}
```

Errors:
- `401 Unauthorized` (`AUTH_002`)

---

## 7. Calculation APIs

### 7.1 계산 실행 + 기록 저장
`POST /api/v1/calculations`

Request:
```json
{
  "firstNumber": 12,
  "secondNumber": 5,
  "operator": "multiply"
}
```

Response `201 Created`:
```json
{
  "historyId": 101,
  "firstNumber": 12,
  "secondNumber": 5,
  "operator": "multiply",
  "result": "60",
  "createdAt": "2026-02-26T04:30:00Z"
}
```

Errors:
- `400 Bad Request` (`CALC_001`, `CALC_002`, `CALC_003`)
- `401 Unauthorized` (`AUTH_002`)

### 7.2 로그인 사용자 계산 기록 조회 (최신순)
`GET /api/v1/calculations/history?limit=20`

Query:
- `limit` (optional): default `20`, max `100`

Response `200 OK`:
```json
{
  "count": 2,
  "items": [
    {
      "historyId": 101,
      "firstNumber": 12,
      "secondNumber": 5,
      "operator": "multiply",
      "result": "60",
      "createdAt": "2026-02-26T04:30:00Z"
    },
    {
      "historyId": 100,
      "firstNumber": 10,
      "secondNumber": 3,
      "operator": "divide",
      "result": "3.3333333333333335",
      "createdAt": "2026-02-26T04:29:00Z"
    }
  ]
}
```

Errors:
- `401 Unauthorized` (`AUTH_002`)

### 7.3 계산 기록 단건 조회
`GET /api/v1/calculations/history/{historyId}`

Response `200 OK`:
```json
{
  "historyId": 101,
  "firstNumber": 12,
  "secondNumber": 5,
  "operator": "multiply",
  "result": "60",
  "createdAt": "2026-02-26T04:30:00Z"
}
```

Errors:
- `401 Unauthorized` (`AUTH_002`)
- `404 Not Found` (`CALC_404`)

---

## 8. Frontend Integration Contract
- 로그인 성공 직후 반드시 `GET /api/v1/calculations/history?limit=20` 호출
- 기록 리스트 클릭 시 기본 동작:
  - `firstNumber` 입력값에 `selectedItem.result` 자동 입력
- 연산 전환 UI는 `add/subtract/multiply/divide` 전부 노출해야 함
- 연산 버튼 전환 시 API `operator` 값이 정확히 매핑되어야 함

## 9. Backward Compatibility
- 기존 `/cal/*` 엔드포인트는 유지한다.
- 신규 회원/기록 기능은 `/api/v1/*`에서 구현한다.
