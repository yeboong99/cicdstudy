package com.neulboong.forcicd.calculator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neulboong.forcicd.calculator.exception.InvalidOperatorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OperatorTests {

	@Test
	@DisplayName("문자열 operator를 enum으로 변환")
	void fromValidOperator() {
		assertThat(Operator.from("add")).isEqualTo(Operator.ADD);
	}

	@Test
	@DisplayName("지원하지 않는 operator 입력 시 명시적 예외 발생")
	void fromInvalidOperator() {
		assertThatThrownBy(() -> Operator.from("mod"))
			.isInstanceOf(InvalidOperatorException.class)
			.hasMessage("지원하지 않는 operator입니다: mod");
	}
}
