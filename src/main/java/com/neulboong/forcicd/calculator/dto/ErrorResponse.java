package com.neulboong.forcicd.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
	private final int status;
	private final String error;
	private final String message;
}
