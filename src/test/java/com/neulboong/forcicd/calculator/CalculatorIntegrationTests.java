package com.neulboong.forcicd.calculator;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Calculator 통합 테스트")
class CalculatorIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Nested
	@DisplayName("REST API 통합 테스트 (/cal/*)")
	class RestApiTests {

		@Test
		@DisplayName("덧셈 API - 전체 흐름 검증")
		void addEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/add")
					.param("firstNumber", "100")
					.param("secondNumber", "200"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(100))
				.andExpect(jsonPath("$.secondNumber").value(200))
				.andExpect(jsonPath("$.operator").value("add"))
				.andExpect(jsonPath("$.result").value("300"));
		}

		@Test
		@DisplayName("뺄셈 API - 전체 흐름 검증")
		void subtractEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/subtract")
					.param("firstNumber", "100")
					.param("secondNumber", "200"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(100))
				.andExpect(jsonPath("$.secondNumber").value(200))
				.andExpect(jsonPath("$.operator").value("subtract"))
				.andExpect(jsonPath("$.result").value("-100"));
		}

		@Test
		@DisplayName("곱셈 API - 전체 흐름 검증")
		void multiplyEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/multiply")
					.param("firstNumber", "12")
					.param("secondNumber", "12"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(12))
				.andExpect(jsonPath("$.secondNumber").value(12))
				.andExpect(jsonPath("$.operator").value("multiply"))
				.andExpect(jsonPath("$.result").value("144"));
		}

		@Test
		@DisplayName("나눗셈 API - 정수 결과")
		void divideExactEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/divide")
					.param("firstNumber", "100")
					.param("secondNumber", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(100))
				.andExpect(jsonPath("$.secondNumber").value(4))
				.andExpect(jsonPath("$.operator").value("divide"))
				.andExpect(jsonPath("$.result").value("25"));
		}

		@Test
		@DisplayName("나눗셈 API - 소수 결과")
		void divideDecimalEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/divide")
					.param("firstNumber", "10")
					.param("secondNumber", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operator").value("divide"))
				.andExpect(jsonPath("$.result").value("3.3333333333333335"));
		}

		@Test
		@DisplayName("나눗셈 API - 0으로 나누기 시 400 에러")
		void divideByZeroEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/divide")
					.param("firstNumber", "10")
					.param("secondNumber", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("0으로 나눌 수 없습니다."));
		}

		@Test
		@DisplayName("음수 연산 통합 검증")
		void negativeNumbersEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/add")
					.param("firstNumber", "-50")
					.param("secondNumber", "-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(-50))
				.andExpect(jsonPath("$.secondNumber").value(-30))
				.andExpect(jsonPath("$.operator").value("add"))
				.andExpect(jsonPath("$.result").value("-80"));
		}

		@Test
		@DisplayName("동적 연산 API - 잘못된 operator 입력 시 400 에러")
		void calculateInvalidOperatorEndToEnd() throws Exception {
			mockMvc.perform(get("/cal/calculate")
					.param("firstNumber", "10")
					.param("secondNumber", "3")
					.param("operator", "mod"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("지원하지 않는 operator입니다: mod"));
		}
	}

	@Nested
	@DisplayName("웹 UI 통합 테스트 (/)")
	class WebUiTests {

		@Test
		@DisplayName("파라미터 없이 홈페이지 접근 시 정상 렌더링")
		void homeWithoutParams() throws Exception {
			mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"));
		}

		@Test
		@DisplayName("덧셈 파라미터로 홈페이지 접근 시 결과 포함")
		void homeWithAddParams() throws Exception {
			mockMvc.perform(get("/")
					.param("firstNumber", "3")
					.param("secondNumber", "5")
					.param("operator", "add"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(model().attribute("result", "8"))
				.andExpect(model().attribute("hasResult", true));
		}

		@Test
		@DisplayName("연산 토글/기록 영역에 Selenium 식별자와 API operator 값이 노출된다")
		void homeContainsStableUiSelectors() throws Exception {
			mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("data-testid=\"first-number-input\"")))
				.andExpect(content().string(containsString("data-testid=\"second-number-input\"")))
				.andExpect(content().string(containsString("data-testid=\"operator-option-add\"")))
				.andExpect(content().string(containsString("data-testid=\"operator-option-subtract\"")))
				.andExpect(content().string(containsString("data-testid=\"operator-option-multiply\"")))
				.andExpect(content().string(containsString("data-testid=\"operator-option-divide\"")))
				.andExpect(content().string(containsString("value=\"add\"")))
				.andExpect(content().string(containsString("value=\"subtract\"")))
				.andExpect(content().string(containsString("value=\"multiply\"")))
				.andExpect(content().string(containsString("value=\"divide\"")))
				.andExpect(content().string(containsString("data-testid=\"history-list\"")))
				.andExpect(content().string(containsString("data-testid=\"clear-history-button\"")));
		}

		@Test
		@DisplayName("0으로 나누기 시 에러 메시지 표시")
		void homeWithDivideByZero() throws Exception {
			mockMvc.perform(get("/")
					.param("firstNumber", "10")
					.param("secondNumber", "0")
					.param("operator", "divide"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("result", "0으로 나눌 수 없습니다."));
		}

		@Test
		@DisplayName("잘못된 operator 입력 시 명시적 에러 메시지 표시")
		void homeWithInvalidOperator() throws Exception {
			mockMvc.perform(get("/")
					.param("firstNumber", "10")
					.param("secondNumber", "3")
					.param("operator", "mod"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("result", "지원하지 않는 operator입니다: mod"))
				.andExpect(model().attribute("hasResult", true));
		}
	}
}
