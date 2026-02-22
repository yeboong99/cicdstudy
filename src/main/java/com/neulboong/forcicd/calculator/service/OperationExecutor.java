package com.neulboong.forcicd.calculator.service;

import org.springframework.stereotype.Component;

import com.neulboong.forcicd.calculator.domain.Operator;

@Component
public class OperationExecutor {

	public Number execute(int firstNumber, int secondNumber, Operator operator) {
		return switch (operator) {
			case ADD -> firstNumber + secondNumber;
			case SUBTRACT -> firstNumber - secondNumber;
			case MULTIPLY -> firstNumber * secondNumber;
			case DIVIDE -> divide(firstNumber, secondNumber);
		};
	}

	private Number divide(int firstNumber, int secondNumber) {
		if (secondNumber == 0) {
			throw new ArithmeticException("0으로 나눌 수 없습니다.");
		}
		double result = (double) firstNumber / secondNumber;
		if (result == (int) result) {
			return (int) result;
		}
		return result;
	}
}
