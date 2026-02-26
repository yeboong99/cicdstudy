package com.neulboong.forcicd.calculator.history.dto;

import java.util.List;

public record CalculationHistoryListResponse(
	int count,
	List<CalculationHistoryItemResponse> items
) {
}
