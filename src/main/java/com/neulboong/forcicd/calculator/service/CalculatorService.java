package com.neulboong.forcicd.calculator.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public class CalculatorService {

	public String addTwoNums(@RequestParam int firstNumber, @RequestParam int secondNumber) {

		int resultInt = 0;
		resultInt = firstNumber + secondNumber;

		return String.valueOf(resultInt);
	}

}
