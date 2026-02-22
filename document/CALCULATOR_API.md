# Calculator API 명세서

> Base URL: `http://{host}:{port}`

## 개요

사칙연산(덧셈, 뺄셈, 곱셈, 나눗셈)을 수행하는 REST API입니다.
모든 엔드포인트는 `GET` 메서드를 사용하며, 쿼리 파라미터로 두 정수를 받습니다.

---

## 공통 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `firstNumber` | `int` | Y | 첫 번째 숫자 |
| `secondNumber` | `int` | Y | 두 번째 숫자 |

---

## 엔드포인트

### 1. 덧셈

```
GET /cal/add?firstNumber={n}&secondNumber={n}
```

**응답**

| 상태 | 본문 | 설명 |
|---|---|---|
| `200 OK` | `"3"` | 두 수의 합 (문자열) |

**예시**

```
GET /cal/add?firstNumber=1&secondNumber=2
→ 200 "3"
```

---

### 2. 뺄셈

```
GET /cal/subtract?firstNumber={n}&secondNumber={n}
```

**응답**

| 상태 | 본문 | 설명 |
|---|---|---|
| `200 OK` | `"-1"` | firstNumber - secondNumber 결과 (문자열) |

**예시**

```
GET /cal/subtract?firstNumber=1&secondNumber=2
→ 200 "-1"
```

---

### 3. 곱셈

```
GET /cal/multiply?firstNumber={n}&secondNumber={n}
```

**응답**

| 상태 | 본문 | 설명 |
|---|---|---|
| `200 OK` | `"6"` | 두 수의 곱 (문자열) |

**예시**

```
GET /cal/multiply?firstNumber=2&secondNumber=3
→ 200 "6"
```

---

### 4. 나눗셈

```
GET /cal/divide?firstNumber={n}&secondNumber={n}
```

**응답**

| 상태 | 본문 | 설명 |
|---|---|---|
| `200 OK` | `"5"` 또는 `"3.3333333333333335"` | 나눗셈 결과. 정수로 나누어 떨어지면 정수, 아니면 소수점 표현 |
| `400 Bad Request` | `"0으로 나눌 수 없습니다."` | secondNumber가 0인 경우 |

**예시**

```
GET /cal/divide?firstNumber=10&secondNumber=2
→ 200 "5"

GET /cal/divide?firstNumber=10&secondNumber=3
→ 200 "3.3333333333333335"

GET /cal/divide?firstNumber=10&secondNumber=0
→ 400 "0으로 나눌 수 없습니다."
```

---

## 웹 UI (Thymeleaf)

```
GET /?firstNumber={n}&secondNumber={n}&operator={op}
```

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `firstNumber` | `Integer` | N | - | 첫 번째 숫자 |
| `secondNumber` | `Integer` | N | - | 두 번째 숫자 |
| `operator` | `String` | N | `add` | 연산 종류: `add`, `subtract`, `multiply`, `divide` |

HTML 페이지(`home.html`)를 렌더링하며, 파라미터가 모두 있으면 계산 결과를 포함합니다.

**Model Attributes:**

| 이름 | 타입 | 설명 |
|---|---|---|
| `result` | `String` | 계산 결과 |
| `hasResult` | `boolean` | 결과 존재 여부 (조건부 렌더링용) |
| `operator` | `String` | 사용된 연산자 |
