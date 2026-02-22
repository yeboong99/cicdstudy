package com.neulboong.forcicd.calculator.controller;

import com.neulboong.forcicd.calculator.service.CalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

	private final CalculatorService calculatorService;

	@GetMapping("/")
	public String home(
		@RequestParam(required = false) Integer firstNumber,
		@RequestParam(required = false) Integer secondNumber,
		@RequestParam(required = false, defaultValue = "add") String operator,
		Model model
	) {
		if (firstNumber != null && secondNumber != null) {
			String result = calculate(firstNumber, secondNumber, operator);
			model.addAttribute("result", result);
			model.addAttribute("hasResult", true);
			model.addAttribute("operator", operator);
		}
		return "home";
	}

	private String calculate(int firstNumber, int secondNumber, String operator) {
		return switch (operator) {
			case "subtract" -> calculatorService.subtractTwoNums(firstNumber, secondNumber);
			case "multiply" -> calculatorService.multiplyTwoNums(firstNumber, secondNumber);
			case "divide" -> {
				try {
					yield calculatorService.divideTwoNums(firstNumber, secondNumber);
				} catch (ArithmeticException e) {
					yield e.getMessage();
				}
			}
			default -> calculatorService.addTwoNums(firstNumber, secondNumber);
		};
	}
}
