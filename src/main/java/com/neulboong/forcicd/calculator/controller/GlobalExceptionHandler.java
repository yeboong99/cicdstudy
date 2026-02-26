package com.neulboong.forcicd.calculator.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.neulboong.forcicd.calculator.dto.ErrorResponse;
import com.neulboong.forcicd.calculator.exception.InvalidOperatorException;
import com.neulboong.forcicd.calculator.history.exception.AuthenticationRequiredException;
import com.neulboong.forcicd.calculator.history.exception.CalculationHistoryNotFoundException;
import com.neulboong.forcicd.calculator.history.exception.CalculationValidationException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
		String message = String.format("'%s' 파라미터의 값 '%s'이(가) 올바른 타입이 아닙니다.", e.getName(), e.getValue());
		return badRequest("CALC_003", message, request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
		String message = String.format("필수 파라미터 '%s'이(가) 누락되었습니다.", e.getParameterName());
		return badRequest("CALC_003", message, request);
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
	public ResponseEntity<ErrorResponse> handleValidation(Exception e, HttpServletRequest request) {
		String message = "요청값 검증에 실패했습니다.";
		if (e instanceof MethodArgumentNotValidException exception && exception.getBindingResult().getFieldError() != null) {
			message = exception.getBindingResult().getFieldError().getDefaultMessage();
		}
		if (e instanceof BindException exception && exception.getBindingResult().getFieldError() != null) {
			message = exception.getBindingResult().getFieldError().getDefaultMessage();
		}
		return badRequest("CALC_003", message, request);
	}

	@ExceptionHandler(CalculationValidationException.class)
	public ResponseEntity<ErrorResponse> handleCalculationValidation(CalculationValidationException e, HttpServletRequest request) {
		return badRequest("CALC_003", e.getMessage(), request);
	}

	@ExceptionHandler(InvalidOperatorException.class)
	public ResponseEntity<ErrorResponse> handleInvalidOperator(InvalidOperatorException e, HttpServletRequest request) {
		return badRequest("CALC_001", e.getMessage(), request);
	}

	@ExceptionHandler(ArithmeticException.class)
	public ResponseEntity<ErrorResponse> handleArithmetic(ArithmeticException e, HttpServletRequest request) {
		return badRequest("CALC_002", e.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationRequiredException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(AuthenticationRequiredException e, HttpServletRequest request) {
		ErrorResponse response = buildError(HttpStatus.UNAUTHORIZED, "AUTH_002", e.getMessage(), request);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(CalculationHistoryNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(CalculationHistoryNotFoundException e, HttpServletRequest request) {
		ErrorResponse response = buildError(HttpStatus.NOT_FOUND, "CALC_404", e.getMessage(), request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest request) {
		ErrorResponse response = buildError(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"COMMON_500",
			"예상하지 못한 오류가 발생했습니다.",
			request
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	private ResponseEntity<ErrorResponse> badRequest(String code, String message, HttpServletRequest request) {
		ErrorResponse response = buildError(HttpStatus.BAD_REQUEST, code, message, request);
		return ResponseEntity.badRequest().body(response);
	}

	private ErrorResponse buildError(HttpStatus status, String code, String message, HttpServletRequest request) {
		return new ErrorResponse(
			status.value(),
			status.getReasonPhrase(),
			code,
			message,
			Instant.now().toString(),
			request.getRequestURI()
		);
	}
}
