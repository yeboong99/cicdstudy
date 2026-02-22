package com.neulboong.forcicd.calculator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neulboong.forcicd.calculator.dto.CalculationResponse;
import com.neulboong.forcicd.calculator.service.CalculatorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cal")
public class CalculatorController {

	private final CalculatorService calculatorService;

	@GetMapping("/add")
	public ResponseEntity<CalculationResponse> addTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		String result = calculatorService.addTwoNums(firstNumber, secondNumber);
		return ResponseEntity.ok(new CalculationResponse(firstNumber, secondNumber, "add", result));
	}

	@GetMapping("/subtract")
	public ResponseEntity<CalculationResponse> subtractTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		String result = calculatorService.subtractTwoNums(firstNumber, secondNumber);
		return ResponseEntity.ok(new CalculationResponse(firstNumber, secondNumber, "subtract", result));
	}

	@GetMapping("/multiply")
	public ResponseEntity<CalculationResponse> multiplyTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		String result = calculatorService.multiplyTwoNums(firstNumber, secondNumber);
		return ResponseEntity.ok(new CalculationResponse(firstNumber, secondNumber, "multiply", result));
	}

	@GetMapping("/divide")
	public ResponseEntity<CalculationResponse> divideTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		String result = calculatorService.divideTwoNums(firstNumber, secondNumber);
		return ResponseEntity.ok(new CalculationResponse(firstNumber, secondNumber, "divide", result));
	}
}
