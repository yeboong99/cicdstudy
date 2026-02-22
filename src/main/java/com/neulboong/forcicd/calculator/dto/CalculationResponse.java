package com.neulboong.forcicd.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CalculationResponse {
	private final int firstNumber;
	private final int secondNumber;
	private final String operator;
	private final String result;
}
