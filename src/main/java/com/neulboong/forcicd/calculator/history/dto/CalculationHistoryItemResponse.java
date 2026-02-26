package com.neulboong.forcicd.calculator.history.dto;

import java.time.Instant;

import com.neulboong.forcicd.calculator.history.domain.CalculationHistory;

public record CalculationHistoryItemResponse(
	Long historyId,
	int firstNumber,
	int secondNumber,
	String operator,
	String result,
	Instant createdAt
) {

	public static CalculationHistoryItemResponse from(CalculationHistory history) {
		return new CalculationHistoryItemResponse(
			history.getHistoryId(),
			history.getFirstNumber(),
			history.getSecondNumber(),
			history.getOperator(),
			history.getResult(),
			history.getCreatedAt()
		);
	}
}
