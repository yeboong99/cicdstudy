package com.neulboong.forcicd.calculator.service;

import org.springframework.stereotype.Component;

@Component
public class ResultFormatter {

	public String format(Number result) {
		return String.valueOf(result);
	}
}
