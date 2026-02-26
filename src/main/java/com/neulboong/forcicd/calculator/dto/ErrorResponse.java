package com.neulboong.forcicd.calculator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
	private final int status;
	private final String error;
	private final String code;
	private final String message;
	private final String timestamp;
	private final String path;

	public ErrorResponse(int status, String error, String message) {
		this(status, error, null, message, null, null);
	}
}
