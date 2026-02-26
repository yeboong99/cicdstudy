package com.neulboong.forcicd.calculator.history.exception;

public class CalculationHistoryNotFoundException extends RuntimeException {

	public CalculationHistoryNotFoundException() {
		super("계산 기록을 찾을 수 없습니다.");
	}
}
