package com.neulboong.forcicd.calculator.service;

import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

	public String addTwoNums(int firstNumber, int secondNumber) {
		int result = firstNumber + secondNumber;
		return String.valueOf(result);
	}

	public String subtractTwoNums(int firstNumber, int secondNumber) {
		int result = firstNumber - secondNumber;
		return String.valueOf(result);
	}

	public String multiplyTwoNums(int firstNumber, int secondNumber) {
		int result = firstNumber * secondNumber;
		return String.valueOf(result);
	}

	public String divideTwoNums(int firstNumber, int secondNumber) {
		if (secondNumber == 0) {
			throw new ArithmeticException("0으로 나눌 수 없습니다.");
		}
		double result = (double) firstNumber / secondNumber;
		if (result == (int) result) {
			return String.valueOf((int) result);
		}
		return String.valueOf(result);
	}
}
