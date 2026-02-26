package com.neulboong.forcicd.calculator.history.exception;

public class AuthenticationRequiredException extends RuntimeException {

	public AuthenticationRequiredException() {
		super("인증되지 않은 사용자입니다.");
	}
}
