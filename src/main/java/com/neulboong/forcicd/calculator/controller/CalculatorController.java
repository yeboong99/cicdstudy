package com.neulboong.forcicd.calculator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neulboong.forcicd.calculator.service.CalculatorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cal")
public class CalculatorController {

	private final CalculatorService calculatorService;

	@GetMapping("/add")
	public ResponseEntity<String> addTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		String result = calculatorService.addTwoNums(firstNumber, secondNumber);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/subtract")
	public ResponseEntity<String> subtractTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		String result = calculatorService.subtractTwoNums(firstNumber, secondNumber);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/multiply")
	public ResponseEntity<String> multiplyTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		String result = calculatorService.multiplyTwoNums(firstNumber, secondNumber);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/divide")
	public ResponseEntity<String> divideTwoNumbers(@RequestParam int firstNumber, @RequestParam int secondNumber) {
		try {
			String result = calculatorService.divideTwoNums(firstNumber, secondNumber);
			return ResponseEntity.ok(result);
		} catch (ArithmeticException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
