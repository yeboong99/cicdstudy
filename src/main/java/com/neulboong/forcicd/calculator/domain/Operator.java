package com.neulboong.forcicd.calculator.domain;

import java.util.Arrays;

import com.neulboong.forcicd.calculator.exception.InvalidOperatorException;

import lombok.Getter;

@Getter
public enum Operator {
	ADD("add"),
	SUBTRACT("subtract"),
	MULTIPLY("multiply"),
	DIVIDE("divide");

	private final String value;

	Operator(String value) {
		this.value = value;
	}

	public static Operator from(String rawOperator) {
		return Arrays.stream(values())
			.filter(operator -> operator.value.equals(rawOperator))
			.findFirst()
			.orElseThrow(() -> new InvalidOperatorException("지원하지 않는 operator입니다: " + rawOperator));
	}
}
