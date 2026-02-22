package com.neulboong.forcicd.calculator.service;

import com.neulboong.forcicd.calculator.domain.Operator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculatorService {

	private final OperationExecutor operationExecutor;
	private final ResultFormatter resultFormatter;

	public String calculate(int firstNumber, int secondNumber, Operator operator) {
		Number result = operationExecutor.execute(firstNumber, secondNumber, operator);
		return resultFormatter.format(result);
	}

	public String addTwoNums(int firstNumber, int secondNumber) {
		return calculate(firstNumber, secondNumber, Operator.ADD);
	}

	public String subtractTwoNums(int firstNumber, int secondNumber) {
		return calculate(firstNumber, secondNumber, Operator.SUBTRACT);
	}

	public String multiplyTwoNums(int firstNumber, int secondNumber) {
		return calculate(firstNumber, secondNumber, Operator.MULTIPLY);
	}

	public String divideTwoNums(int firstNumber, int secondNumber) {
		return calculate(firstNumber, secondNumber, Operator.DIVIDE);
	}
}
