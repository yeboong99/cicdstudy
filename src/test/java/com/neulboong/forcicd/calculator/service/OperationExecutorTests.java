package com.neulboong.forcicd.calculator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neulboong.forcicd.calculator.domain.Operator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OperationExecutorTests {

	private final OperationExecutor operationExecutor = new OperationExecutor();

	@Test
	@DisplayName("정수 나눗셈 결과는 Integer로 반환")
	void divideExactReturnsInteger() {
		Number result = operationExecutor.execute(10, 2, Operator.DIVIDE);
		assertThat(result).isInstanceOf(Integer.class);
		assertThat(result.intValue()).isEqualTo(5);
	}

	@Test
	@DisplayName("소수 나눗셈 결과는 Double로 반환")
	void divideDecimalReturnsDouble() {
		Number result = operationExecutor.execute(10, 3, Operator.DIVIDE);
		assertThat(result).isInstanceOf(Double.class);
		assertThat(result.doubleValue()).isEqualTo(10.0 / 3);
	}

	@Test
	@DisplayName("0으로 나누기 시 ArithmeticException 발생")
	void divideByZeroThrows() {
		assertThatThrownBy(() -> operationExecutor.execute(10, 0, Operator.DIVIDE))
			.isInstanceOf(ArithmeticException.class)
			.hasMessage("0으로 나눌 수 없습니다.");
	}
}
