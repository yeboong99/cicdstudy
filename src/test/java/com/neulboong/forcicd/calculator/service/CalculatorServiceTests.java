package com.neulboong.forcicd.calculator.service;

import static org.assertj.core.api.Assertions.*;

import com.neulboong.forcicd.calculator.domain.Operator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculatorServiceTests {

	private CalculatorService calculatorService;

	@BeforeEach
	void setUp() {
		calculatorService = new CalculatorService(new OperationExecutor(), new ResultFormatter());
	}

	@Nested
	@DisplayName("덧셈 (addTwoNums)")
	class AddTests {

		@Test
		@DisplayName("양수 + 양수")
		void addPositiveNumbers() {
			assertThat(calculatorService.addTwoNums(3, 5)).isEqualTo("8");
		}

		@Test
		@DisplayName("음수 + 음수")
		void addNegativeNumbers() {
			assertThat(calculatorService.addTwoNums(-3, -5)).isEqualTo("-8");
		}

		@Test
		@DisplayName("양수 + 음수")
		void addPositiveAndNegative() {
			assertThat(calculatorService.addTwoNums(10, -3)).isEqualTo("7");
		}

		@Test
		@DisplayName("0 + 0")
		void addZeros() {
			assertThat(calculatorService.addTwoNums(0, 0)).isEqualTo("0");
		}

		@Test
		@DisplayName("큰 수 덧셈 (오버플로우 경계)")
		void addLargeNumbers() {
			assertThat(calculatorService.addTwoNums(Integer.MAX_VALUE, 0))
				.isEqualTo(String.valueOf(Integer.MAX_VALUE));
		}
	}

	@Nested
	@DisplayName("뺄셈 (subtractTwoNums)")
	class SubtractTests {

		@Test
		@DisplayName("큰 수 - 작은 수 = 양수")
		void subtractResultPositive() {
			assertThat(calculatorService.subtractTwoNums(10, 3)).isEqualTo("7");
		}

		@Test
		@DisplayName("작은 수 - 큰 수 = 음수")
		void subtractResultNegative() {
			assertThat(calculatorService.subtractTwoNums(3, 10)).isEqualTo("-7");
		}

		@Test
		@DisplayName("같은 수 뺄셈 = 0")
		void subtractSameNumbers() {
			assertThat(calculatorService.subtractTwoNums(5, 5)).isEqualTo("0");
		}

		@Test
		@DisplayName("음수 - 음수")
		void subtractNegativeNumbers() {
			assertThat(calculatorService.subtractTwoNums(-3, -5)).isEqualTo("2");
		}
	}

	@Nested
	@DisplayName("곱셈 (multiplyTwoNums)")
	class MultiplyTests {

		@Test
		@DisplayName("양수 * 양수")
		void multiplyPositiveNumbers() {
			assertThat(calculatorService.multiplyTwoNums(3, 5)).isEqualTo("15");
		}

		@Test
		@DisplayName("양수 * 음수 = 음수")
		void multiplyPositiveAndNegative() {
			assertThat(calculatorService.multiplyTwoNums(3, -5)).isEqualTo("-15");
		}

		@Test
		@DisplayName("음수 * 음수 = 양수")
		void multiplyNegativeNumbers() {
			assertThat(calculatorService.multiplyTwoNums(-3, -5)).isEqualTo("15");
		}

		@Test
		@DisplayName("0을 곱하면 0")
		void multiplyByZero() {
			assertThat(calculatorService.multiplyTwoNums(100, 0)).isEqualTo("0");
		}

		@Test
		@DisplayName("1을 곱하면 원래 값")
		void multiplyByOne() {
			assertThat(calculatorService.multiplyTwoNums(42, 1)).isEqualTo("42");
		}
	}

	@Nested
	@DisplayName("나눗셈 (divideTwoNums)")
	class DivideTests {

		@Test
		@DisplayName("나누어 떨어지는 경우 정수 반환")
		void divideExactly() {
			assertThat(calculatorService.divideTwoNums(10, 2)).isEqualTo("5");
		}

		@Test
		@DisplayName("나누어 떨어지지 않는 경우 소수 반환")
		void divideWithRemainder() {
			assertThat(calculatorService.divideTwoNums(10, 3)).isEqualTo(String.valueOf(10.0 / 3));
		}

		@Test
		@DisplayName("음수 나눗셈")
		void divideNegativeNumbers() {
			assertThat(calculatorService.divideTwoNums(-10, 2)).isEqualTo("-5");
		}

		@Test
		@DisplayName("0을 나누면 0")
		void divideZeroByNumber() {
			assertThat(calculatorService.divideTwoNums(0, 5)).isEqualTo("0");
		}

		@Test
		@DisplayName("0으로 나누면 ArithmeticException 발생")
		void divideByZero() {
			assertThatThrownBy(() -> calculatorService.divideTwoNums(10, 0))
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("0으로 나눌 수 없습니다.");
		}
	}

	@Nested
	@DisplayName("통합 연산 진입점 (calculate)")
	class CalculateTests {

		@Test
		@DisplayName("operator enum을 통해 연산 수행")
		void calculateWithOperatorEnum() {
			assertThat(calculatorService.calculate(8, 2, Operator.DIVIDE)).isEqualTo("4");
		}
	}
}
