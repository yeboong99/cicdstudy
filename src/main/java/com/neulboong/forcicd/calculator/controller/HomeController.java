package com.neulboong.forcicd.calculator.controller;

import com.neulboong.forcicd.calculator.domain.Operator;
import com.neulboong.forcicd.calculator.exception.InvalidOperatorException;
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
			try {
				String result = calculate(firstNumber, secondNumber, operator);
				model.addAttribute("result", result);
				model.addAttribute("hasResult", true);
				model.addAttribute("operator", operator);
			} catch (ArithmeticException | InvalidOperatorException e) {
				model.addAttribute("result", e.getMessage());
				model.addAttribute("hasResult", true);
				model.addAttribute("operator", operator);
			}
		}
		return "home";
	}

	private String calculate(int firstNumber, int secondNumber, String operator) {
		Operator parsedOperator = Operator.from(operator);
		return calculatorService.calculate(firstNumber, secondNumber, parsedOperator);
	}
}
