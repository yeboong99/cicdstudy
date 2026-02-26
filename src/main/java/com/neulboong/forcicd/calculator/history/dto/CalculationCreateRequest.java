package com.neulboong.forcicd.calculator.history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CalculationCreateRequest(
	@NotNull(message = "firstNumber는 필수입니다.") Integer firstNumber,
	@NotNull(message = "secondNumber는 필수입니다.") Integer secondNumber,
	@NotBlank(message = "operator는 필수입니다.") String operator
) {
}
