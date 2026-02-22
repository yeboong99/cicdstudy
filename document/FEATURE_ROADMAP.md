# FEATURE_ROADMAP

## 0) 목표와 전제
- 목표: 현재 사칙연산 중심 서비스를 "학습용 + 실사용 + 운영 가능" 제품으로 확장한다.
- 전제: 기존 `/cal/*` 엔드포인트와 Thymeleaf UI는 유지하고, 신규 API를 점진적으로 추가한다.
- 점수 기준(1~5):
  - Impact: 사용자/비즈니스 가치(높을수록 좋음)
  - Effort: 구현 난이도/기간(높을수록 어려움)
  - Risk: 장애/보안/운영 리스크(높을수록 위험)
- 우선순위 산식: `PriorityScore = (Impact * 2) - Effort - Risk`

## 1) 사용자 시나리오 3개
### 시나리오 A. 학습용(학생)
- 고등학생이 웹 UI에서 덧셈/나눗셈과 수식 계산을 연습한다.
- 계산 이력을 다시 보며 어떤 연산에서 실수했는지 확인한다.
- 자주 푸는 문제 유형을 템플릿으로 저장해 반복 학습한다.

### 시나리오 B. 실사용(일반 사용자/팀)
- 소규모 쇼핑몰 운영자가 가격, 할인율, 수량 계산을 반복 수행한다.
- REST API로 여러 계산을 한 번에 처리해 엑셀 수작업을 줄인다.
- 최근 계산 결과를 조회/재실행해 실수 없이 업무를 재현한다.

### 시나리오 C. 운영자(SRE/개발자)
- 운영자는 API 키별 사용량과 에러율을 확인한다.
- 특정 클라이언트가 과도 호출 시 rate limit로 보호한다.
- 장애 시 요청 이력과 에러코드를 통해 원인을 빠르게 파악한다.

## 2) 신규 기능 아이디어 8개 + 점수
| ID | 기능 아이디어 | 핵심 가치 | Impact | Effort | Risk | PriorityScore |
|---|---|---|---:|---:|---:|---:|
| F1 | 계산 이력 저장/조회/재실행 | 학습 복기 + 업무 재현성 향상 | 5 | 3 | 2 | 5 |
| F2 | 수식 문자열 계산(괄호/우선순위) | 사칙 API 한계 해소, 실사용성 확대 | 5 | 4 | 3 | 3 |
| F3 | 배치 계산 API(한 요청에 N건) | 반복 계산 자동화, API 효율 개선 | 4 | 3 | 2 | 3 |
| F4 | 즐겨찾기/템플릿 계산식 | 반복 작업 속도 개선 | 3 | 3 | 2 | 1 |
| F5 | API Key 발급/관리 + 기본 인증 | 외부 사용 통제, 고객별 관리 기반 | 4 | 4 | 3 | 1 |
| F6 | 클라이언트별 Rate Limit/Quota | 과호출 방지, 안정성 확보 | 4 | 4 | 4 | 0 |
| F7 | 운영 대시보드(요청량/에러율/지연) | 운영 가시성, 장애 대응 시간 단축 | 4 | 3 | 3 | 2 |
| F8 | 표준 에러코드 + API 버전(v1) 정리 | 통합/운영 비용 절감 | 3 | 2 | 2 | 2 |

## 3) 최우선 3개 기능 선정
### 1. F1 계산 이력 저장/조회/재실행
- 이유: 학습/실사용/운영자 모두에게 즉시 가치를 주는 공통 기반 기능이다.
- 이유: 이후 기능(F7 분석, F4 템플릿, 리포팅)의 데이터 기반이 된다.

### 2. F2 수식 문자열 계산(괄호/우선순위)
- 이유: 현재 2개 숫자 기반 API의 한계를 직접 해결해 제품 체감가치를 크게 높인다.
- 이유: 실사용 시나리오(할인, 정산, 중첩 계산)에 바로 연결된다.

### 3. F5 API Key 발급/관리 + 기본 인증
- 이유: 외부 연동을 시작하기 위한 최소 운영 통제 장치다.
- 이유: F6(rate limit), F7(운영 관측)의 선행 조건으로 확장성이 높다.

## 4) 최우선 3개 상세 설계
### F1 계산 이력 저장/조회/재실행
#### API 스케치
- `POST /api/v1/calculations`
  - request:
    ```json
    {
      "operandA": "12000",
      "operandB": "15",
      "operator": "multiply",
      "source": "web"
    }
    ```
  - response:
    ```json
    {
      "calculationId": "cal_20260222_001",
      "result": "180000",
      "createdAt": "2026-02-22T11:30:00Z"
    }
    ```
- `GET /api/v1/calculations?actorId=u_1001&cursor=...&limit=20`
- `POST /api/v1/calculations/{calculationId}/replay`

#### 데이터 모델 초안
- `calculation_records`
  - `id` (PK, bigint)
  - `calculation_uid` (varchar, unique)
  - `actor_id` (varchar, nullable)
  - `source` (enum: `web`, `api`)
  - `operator` (enum: `add`, `subtract`, `multiply`, `divide`, `expression`)
  - `operand_a` (varchar)
  - `operand_b` (varchar, nullable)
  - `expression` (text, nullable)
  - `result` (varchar)
  - `status` (enum: `success`, `error`)
  - `error_code` (varchar, nullable)
  - `created_at` (timestamp)

#### 로드맵
- 2주:
  - 이력 테이블 + 기본 CRUD API
  - 기존 `/cal/*` 결과 저장 훅 추가
  - 최근 20건 조회 UI 섹션(간단 목록)
- 4주:
  - 재실행 API + UI 버튼
  - 필터(연산자/기간), 페이지네이션
  - 실패 케이스(error_code) 저장/조회

### F2 수식 문자열 계산(괄호/우선순위)
#### API 스케치
- `POST /api/v1/expressions/evaluate`
  - request:
    ```json
    {
      "expression": "(12000 * 0.9) + 2500 / 5",
      "precision": 6
    }
    ```
  - response:
    ```json
    {
      "normalizedExpression": "(12000*0.9)+2500/5",
      "result": "11300",
      "durationMs": 4
    }
    ```
- `POST /api/v1/expressions/validate`

#### 데이터 모델 초안
- `expression_evaluations`
  - `id` (PK)
  - `expression_raw` (text)
  - `expression_normalized` (text)
  - `precision` (int)
  - `result` (varchar)
  - `status` (enum: `success`, `error`)
  - `error_code` (varchar, nullable)
  - `created_at` (timestamp)

#### 로드맵
- 2주:
  - 토크나이저/파서(+, -, *, /, 괄호)
  - evaluate/validate API + 에러코드 정의
  - 단위 테스트(정상/오입력/0나눗셈)
- 4주:
  - 소수 정밀도 제어
  - 기존 이력(F1)과 연동 저장
  - UI 수식 입력 모드 추가

### F5 API Key 발급/관리 + 기본 인증
#### API 스케치
- `POST /api/v1/admin/api-keys`
  - request:
    ```json
    {
      "clientName": "my-shop",
      "plan": "basic"
    }
    ```
  - response:
    ```json
    {
      "clientId": "cli_001",
      "apiKey": "fc_live_xxxxxxxxx",
      "createdAt": "2026-02-22T11:35:00Z"
    }
    ```
- `GET /api/v1/admin/api-keys?status=active`
- `POST /api/v1/admin/api-keys/{clientId}/revoke`
- 공통: `X-API-Key` 헤더 필수(신규 `/api/v1/*` 경로 대상)

#### 데이터 모델 초안
- `api_clients`
  - `id` (PK)
  - `client_uid` (varchar, unique)
  - `client_name` (varchar)
  - `plan` (enum: `basic`, `pro`)
  - `status` (enum: `active`, `revoked`)
  - `created_at` (timestamp)
- `api_keys`
  - `id` (PK)
  - `client_id` (FK -> api_clients.id)
  - `key_hash` (varchar)
  - `last_used_at` (timestamp, nullable)
  - `expires_at` (timestamp, nullable)
  - `created_at` (timestamp)

#### 로드맵
- 2주:
  - API key 발급/폐기 Admin API
  - 인증 필터(`X-API-Key`) 및 해시 저장
  - 감사 로그(발급/폐기 이벤트)
- 4주:
  - 키 로테이션(재발급) 지원
  - 클라이언트별 기본 사용량 통계(일 단위)
  - 관리 화면(키 상태 조회)

## 5) 전체 실행 우선순위 제안
1. F1(계산 이력): 데이터 기반 확보
2. F2(수식 계산): 사용자 가치 극대화
3. F5(API 키): 외부 연동 운영 안정성 확보
4. F7(운영 대시보드): 운영 가시성 보강
5. F3(배치 계산): API 효율화
6. F8(버전/에러 표준화): 장기 유지보수 기반
7. F6(고급 rate limit): 고트래픽 대비
8. F4(템플릿): UX 고도화
