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
}
