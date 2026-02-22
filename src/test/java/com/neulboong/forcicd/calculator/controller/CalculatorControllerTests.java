package com.neulboong.forcicd.calculator.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.neulboong.forcicd.calculator.service.CalculatorService;

@WebMvcTest(CalculatorController.class)
class CalculatorControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CalculatorService calculatorService;

	@Nested
	@DisplayName("GET /cal/add")
	class AddEndpoint {

		@Test
		@DisplayName("정상 요청 시 200 OK와 구조화된 JSON 반환")
		void addSuccess() throws Exception {
			given(calculatorService.addTwoNums(3, 5)).willReturn("8");

			mockMvc.perform(get("/cal/add")
					.param("firstNumber", "3")
					.param("secondNumber", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(3))
				.andExpect(jsonPath("$.secondNumber").value(5))
				.andExpect(jsonPath("$.operator").value("add"))
				.andExpect(jsonPath("$.result").value("8"));
		}

		@Test
		@DisplayName("파라미터 누락 시 400 에러")
		void addMissingParam() throws Exception {
			mockMvc.perform(get("/cal/add")
					.param("firstNumber", "3"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"));
		}
	}

	@Nested
	@DisplayName("GET /cal/subtract")
	class SubtractEndpoint {

		@Test
		@DisplayName("정상 요청 시 200 OK와 구조화된 JSON 반환")
		void subtractSuccess() throws Exception {
			given(calculatorService.subtractTwoNums(10, 3)).willReturn("7");

			mockMvc.perform(get("/cal/subtract")
					.param("firstNumber", "10")
					.param("secondNumber", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(10))
				.andExpect(jsonPath("$.secondNumber").value(3))
				.andExpect(jsonPath("$.operator").value("subtract"))
				.andExpect(jsonPath("$.result").value("7"));
		}
	}

	@Nested
	@DisplayName("GET /cal/multiply")
	class MultiplyEndpoint {

		@Test
		@DisplayName("정상 요청 시 200 OK와 구조화된 JSON 반환")
		void multiplySuccess() throws Exception {
			given(calculatorService.multiplyTwoNums(4, 5)).willReturn("20");

			mockMvc.perform(get("/cal/multiply")
					.param("firstNumber", "4")
					.param("secondNumber", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(4))
				.andExpect(jsonPath("$.secondNumber").value(5))
				.andExpect(jsonPath("$.operator").value("multiply"))
				.andExpect(jsonPath("$.result").value("20"));
		}
	}

	@Nested
	@DisplayName("GET /cal/divide")
	class DivideEndpoint {

		@Test
		@DisplayName("정상 요청 시 200 OK와 구조화된 JSON 반환")
		void divideSuccess() throws Exception {
			given(calculatorService.divideTwoNums(10, 2)).willReturn("5");

			mockMvc.perform(get("/cal/divide")
					.param("firstNumber", "10")
					.param("secondNumber", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstNumber").value(10))
				.andExpect(jsonPath("$.secondNumber").value(2))
				.andExpect(jsonPath("$.operator").value("divide"))
				.andExpect(jsonPath("$.result").value("5"));
		}

		@Test
		@DisplayName("0으로 나누기 시 400 에러와 에러 응답 반환")
		void divideByZero() throws Exception {
			given(calculatorService.divideTwoNums(10, 0))
				.willThrow(new ArithmeticException("0으로 나눌 수 없습니다."));

			mockMvc.perform(get("/cal/divide")
					.param("firstNumber", "10")
					.param("secondNumber", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("0으로 나눌 수 없습니다."));
		}
	}
}
