# Calculator 도메인 테스트 결과 보고서

## 개요

Calculator 도메인의 사칙연산(덧셈, 뺄셈, 곱셈, 나눗셈) 기능에 대한 단위 테스트와 통합 테스트를 작성하고 실행한 결과입니다.

- 테스트 실행일: 2026-02-22
- Spring Boot: 4.0.2 / Java 17 / JUnit 5
- 테스트 DB: H2 (인메모리)

---

## 테스트 결과 요약

| 구분 | 테스트 수 | 성공 | 실패 | 건너뜀 |
|------|----------|------|------|--------|
| CalculatorService 단위 테스트 | 19 | 19 | 0 | 0 |
| CalculatorController 단위 테스트 | 6 | 6 | 0 | 0 |
| Calculator 통합 테스트 | 10 | 10 | 0 | 0 |
| **합계** | **35** | **35** | **0** | **0** |

**전체 테스트 통과율: 100%**

---

## 1. CalculatorService 단위 테스트

**파일:** `src/test/java/com/neulboong/forcicd/calculator/service/CalculatorServiceTests.java`

Spring 컨텍스트 없이 순수 Java 단위 테스트로 작성했습니다. `CalculatorService`를 직접 생성하여 각 메서드의 비즈니스 로직을 검증합니다.

### 1.1 덧셈 (addTwoNums) - 5개 테스트

| 테스트명 | 입력 | 기대값 | 결과 |
|---------|------|--------|------|
| 양수 + 양수 | 3, 5 | "8" | PASS |
| 음수 + 음수 | -3, -5 | "-8" | PASS |
| 양수 + 음수 | 10, -3 | "7" | PASS |
| 0 + 0 | 0, 0 | "0" | PASS |
| 큰 수 덧셈 (오버플로우 경계) | Integer.MAX_VALUE, 0 | "2147483647" | PASS |

### 1.2 뺄셈 (subtractTwoNums) - 4개 테스트

| 테스트명 | 입력 | 기대값 | 결과 |
|---------|------|--------|------|
| 큰 수 - 작은 수 = 양수 | 10, 3 | "7" | PASS |
| 작은 수 - 큰 수 = 음수 | 3, 10 | "-7" | PASS |
| 같은 수 뺄셈 = 0 | 5, 5 | "0" | PASS |
| 음수 - 음수 | -3, -5 | "2" | PASS |

### 1.3 곱셈 (multiplyTwoNums) - 5개 테스트

| 테스트명 | 입력 | 기대값 | 결과 |
|---------|------|--------|------|
| 양수 * 양수 | 3, 5 | "15" | PASS |
| 양수 * 음수 = 음수 | 3, -5 | "-15" | PASS |
| 음수 * 음수 = 양수 | -3, -5 | "15" | PASS |
| 0을 곱하면 0 | 100, 0 | "0" | PASS |
| 1을 곱하면 원래 값 | 42, 1 | "42" | PASS |

### 1.4 나눗셈 (divideTwoNums) - 5개 테스트

| 테스트명 | 입력 | 기대값 | 결과 |
|---------|------|--------|------|
| 나누어 떨어지는 경우 정수 반환 | 10, 2 | "5" | PASS |
| 나누어 떨어지지 않는 경우 소수 반환 | 10, 3 | "3.3333333333333335" | PASS |
| 음수 나눗셈 | -10, 2 | "-5" | PASS |
| 0을 나누면 0 | 0, 5 | "0" | PASS |
| 0으로 나누면 ArithmeticException 발생 | 10, 0 | ArithmeticException("0으로 나눌 수 없습니다.") | PASS |

---

## 2. CalculatorController 단위 테스트

**파일:** `src/test/java/com/neulboong/forcicd/calculator/controller/CalculatorControllerTests.java`

`@WebMvcTest`와 `@MockitoBean`을 사용하여 Controller 레이어만 격리 테스트합니다. Service는 Mock 처리하여 HTTP 요청/응답 처리 로직만 검증합니다.

### 2.1 GET /cal/add - 2개 테스트

| 테스트명 | 요청 | 기대 응답 | 결과 |
|---------|------|----------|------|
| 정상 요청 시 200 OK와 결과 반환 | `?firstNumber=3&secondNumber=5` | 200 OK, "8" | PASS |
| 파라미터 누락 시 400 에러 | `?firstNumber=3` (secondNumber 누락) | 400 Bad Request | PASS |

### 2.2 GET /cal/subtract - 1개 테스트

| 테스트명 | 요청 | 기대 응답 | 결과 |
|---------|------|----------|------|
| 정상 요청 시 200 OK와 결과 반환 | `?firstNumber=10&secondNumber=3` | 200 OK, "7" | PASS |

### 2.3 GET /cal/multiply - 1개 테스트

| 테스트명 | 요청 | 기대 응답 | 결과 |
|---------|------|----------|------|
| 정상 요청 시 200 OK와 결과 반환 | `?firstNumber=4&secondNumber=5` | 200 OK, "20" | PASS |

### 2.4 GET /cal/divide - 2개 테스트

| 테스트명 | 요청 | 기대 응답 | 결과 |
|---------|------|----------|------|
| 정상 요청 시 200 OK와 결과 반환 | `?firstNumber=10&secondNumber=2` | 200 OK, "5" | PASS |
| 0으로 나누기 시 400 에러와 메시지 반환 | `?firstNumber=10&secondNumber=0` | 400 Bad Request, "0으로 나눌 수 없습니다." | PASS |

---

## 3. Calculator 통합 테스트

**파일:** `src/test/java/com/neulboong/forcicd/calculator/CalculatorIntegrationTests.java`

`@SpringBootTest` + `@AutoConfigureMockMvc`를 사용하여 전체 애플리케이션 컨텍스트를 로드한 상태에서 Controller → Service 전체 흐름을 검증합니다.

### 3.1 REST API 통합 테스트 (/cal/*) - 7개 테스트

| 테스트명 | 요청 | 기대 응답 | 결과 |
|---------|------|----------|------|
| 덧셈 API - 전체 흐름 검증 | `/cal/add?firstNumber=100&secondNumber=200` | 200, "300" | PASS |
| 뺄셈 API - 전체 흐름 검증 | `/cal/subtract?firstNumber=100&secondNumber=200` | 200, "-100" | PASS |
| 곱셈 API - 전체 흐름 검증 | `/cal/multiply?firstNumber=12&secondNumber=12` | 200, "144" | PASS |
| 나눗셈 API - 정수 결과 | `/cal/divide?firstNumber=100&secondNumber=4` | 200, "25" | PASS |
| 나눗셈 API - 소수 결과 | `/cal/divide?firstNumber=10&secondNumber=3` | 200, ~3.3333 | PASS |
| 나눗셈 API - 0으로 나누기 시 400 에러 | `/cal/divide?firstNumber=10&secondNumber=0` | 400, "0으로 나눌 수 없습니다." | PASS |
| 음수 연산 통합 검증 | `/cal/add?firstNumber=-50&secondNumber=-30` | 200, "-80" | PASS |

### 3.2 웹 UI 통합 테스트 (/) - 3개 테스트

| 테스트명 | 요청 | 기대 응답 | 결과 |
|---------|------|----------|------|
| 파라미터 없이 홈페이지 접근 시 정상 렌더링 | `/` | 200, view="home" | PASS |
| 덧셈 파라미터로 홈페이지 접근 시 결과 포함 | `/?firstNumber=3&secondNumber=5&operator=add` | model: result="8", hasResult=true | PASS |
| 0으로 나누기 시 에러 메시지 표시 | `/?firstNumber=10&secondNumber=0&operator=divide` | model: result="0으로 나눌 수 없습니다." | PASS |

---

## 테스트 전략

### 단위 테스트 (Unit Test)
- **CalculatorServiceTests**: Spring 컨텍스트 없이 순수 로직 검증. 정상 케이스, 경계값(0, 음수, 큰 수), 예외 상황(0으로 나누기)을 커버.
- **CalculatorControllerTests**: `@WebMvcTest`로 Controller 레이어 격리. Service를 Mock 처리하여 HTTP 요청 파싱, 응답 생성, 에러 핸들링만 검증.

### 통합 테스트 (Integration Test)
- **CalculatorIntegrationTests**: `@SpringBootTest`로 전체 컨텍스트 로드. Controller → Service 연동이 올바르게 동작하는지 End-to-End 검증. REST API와 웹 UI 모두 커버.

### 커버리지 영역
- 사칙연산 4개 메서드의 정상/비정상 케이스
- REST API 엔드포인트 4개 (/cal/add, /cal/subtract, /cal/multiply, /cal/divide)
- 웹 UI 엔드포인트 (/) - HomeController의 계산기 기능
- 에러 핸들링 (0으로 나누기, 파라미터 누락)
