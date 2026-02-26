package com.neulboong.forcicd.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neulboong.forcicd.auth.domain.Member;
import com.neulboong.forcicd.auth.dto.AuthMessageResponse;
import com.neulboong.forcicd.auth.dto.AuthRequest;
import com.neulboong.forcicd.auth.dto.AuthUserResponse;
import com.neulboong.forcicd.auth.exception.UnauthorizedException;
import com.neulboong.forcicd.auth.service.AuthService;
import com.neulboong.forcicd.auth.session.SessionKeys;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<AuthUserResponse> signup(@RequestBody AuthRequest request) {
		Member member = authService.signup(request.username(), request.password());
		return ResponseEntity.status(HttpStatus.CREATED).body(AuthUserResponse.from(member));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthUserResponse> login(@RequestBody AuthRequest request, HttpServletRequest httpServletRequest) {
		Member member = authService.authenticate(request.username(), request.password());

		HttpSession existingSession = httpServletRequest.getSession(false);
		if (existingSession != null) {
			existingSession.invalidate();
		}

		HttpSession session = httpServletRequest.getSession(true);
		session.setAttribute(SessionKeys.AUTH_USER_ID, member.getId());

		return ResponseEntity.ok(AuthUserResponse.from(member));
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthMessageResponse> logout(HttpServletRequest httpServletRequest) {
		HttpSession session = getAuthenticatedSession(httpServletRequest);
		session.invalidate();
		return ResponseEntity.ok(new AuthMessageResponse("로그아웃되었습니다."));
	}

	@GetMapping("/me")
	public ResponseEntity<AuthUserResponse> me(HttpServletRequest httpServletRequest) {
		HttpSession session = getAuthenticatedSession(httpServletRequest);
		Object memberIdAttr = session.getAttribute(SessionKeys.AUTH_USER_ID);
		if (!(memberIdAttr instanceof Long memberId)) {
			throw new UnauthorizedException("인증이 필요합니다.");
		}

		Member member = authService.getMemberById(memberId);
		return ResponseEntity.ok(AuthUserResponse.from(member));
	}

	private HttpSession getAuthenticatedSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(SessionKeys.AUTH_USER_ID) == null) {
			throw new UnauthorizedException("인증이 필요합니다.");
		}
		return session;
	}
}
