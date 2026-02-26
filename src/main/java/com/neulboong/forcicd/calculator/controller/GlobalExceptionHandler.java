package com.neulboong.forcicd.calculator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.neulboong.forcicd.auth.exception.AuthenticationFailedException;
import com.neulboong.forcicd.auth.exception.DuplicateUsernameException;
import com.neulboong.forcicd.auth.exception.UnauthorizedException;
import com.neulboong.forcicd.calculator.dto.ErrorResponse;
import com.neulboong.forcicd.calculator.exception.InvalidOperatorException;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		String message = String.format("'%s' 파라미터의 값 '%s'이(가) 올바른 타입이 아닙니다.", e.getName(), e.getValue());
		return badRequest(message);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
		String message = String.format("필수 파라미터 '%s'이(가) 누락되었습니다.", e.getParameterName());
		return badRequest(message);
	}

	@ExceptionHandler(InvalidOperatorException.class)
	public ResponseEntity<ErrorResponse> handleInvalidOperator(InvalidOperatorException e) {
		return badRequest(e.getMessage());
	}

	@ExceptionHandler(ArithmeticException.class)
	public ResponseEntity<ErrorResponse> handleArithmetic(ArithmeticException e) {
		return badRequest(e.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleInvalidBody(HttpMessageNotReadableException e) {
		return badRequest("요청 본문 형식이 올바르지 않습니다.");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
		return badRequest(e.getMessage());
	}

	@ExceptionHandler(DuplicateUsernameException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateUsername(DuplicateUsernameException e) {
		return error(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler({AuthenticationFailedException.class, UnauthorizedException.class})
	public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException e) {
		return error(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "예상하지 못한 오류가 발생했습니다.");
	}

	private ResponseEntity<ErrorResponse> badRequest(String message) {
		return error(HttpStatus.BAD_REQUEST, message);
	}

	private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
		ErrorResponse response = new ErrorResponse(status.value(), status.getReasonPhrase(), message);
		return ResponseEntity.status(status).body(response);
	}
}
