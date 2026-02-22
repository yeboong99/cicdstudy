package com.neulboong.forcicd.calculator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.neulboong.forcicd.calculator.dto.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		String message = String.format("'%s' 파라미터의 값 '%s'이(가) 올바른 타입이 아닙니다.", e.getName(), e.getValue());
		ErrorResponse response = new ErrorResponse(400, "Bad Request", message);
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
		String message = String.format("필수 파라미터 '%s'이(가) 누락되었습니다.", e.getParameterName());
		ErrorResponse response = new ErrorResponse(400, "Bad Request", message);
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(ArithmeticException.class)
	public ResponseEntity<ErrorResponse> handleArithmetic(ArithmeticException e) {
		ErrorResponse response = new ErrorResponse(400, "Bad Request", e.getMessage());
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
		ErrorResponse response = new ErrorResponse(500, "Internal Server Error", "예상하지 못한 오류가 발생했습니다.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}
