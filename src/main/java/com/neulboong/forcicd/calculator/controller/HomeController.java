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
		Model model
	) {
		if (firstNumber != null && secondNumber != null) {
			String result = calculatorService.addTwoNums(firstNumber, secondNumber);
			model.addAttribute("result", result);
			model.addAttribute("hasResult", true);
		}
		return "home";
	}
}
