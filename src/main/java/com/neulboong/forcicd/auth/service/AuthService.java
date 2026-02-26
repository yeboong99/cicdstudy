package com.neulboong.forcicd.auth.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neulboong.forcicd.auth.domain.Member;
import com.neulboong.forcicd.auth.exception.AuthenticationFailedException;
import com.neulboong.forcicd.auth.exception.DuplicateUsernameException;
import com.neulboong.forcicd.auth.exception.UnauthorizedException;
import com.neulboong.forcicd.auth.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private static final String INVALID_CREDENTIALS_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다.";

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public Member signup(String username, String rawPassword) {
		String normalizedUsername = normalizeUsername(username);
		validatePassword(rawPassword);

		Optional<Member> existing = memberRepository.findByUsername(normalizedUsername);
		if (existing.isPresent()) {
			throw new DuplicateUsernameException("이미 사용 중인 username입니다.");
		}

		String hashedPassword = passwordEncoder.encode(rawPassword);

		try {
			return memberRepository.save(new Member(normalizedUsername, hashedPassword));
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateUsernameException("이미 사용 중인 username입니다.");
		}
	}

	public Member authenticate(String username, String rawPassword) {
		String normalizedUsername = normalizeUsername(username);
		validatePassword(rawPassword);

		Member member = memberRepository.findByUsername(normalizedUsername)
			.orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE));

		if (!passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
			throw new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE);
		}

		return member;
	}

	public Member getMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new UnauthorizedException("인증이 필요합니다."));
	}

	private String normalizeUsername(String username) {
		if (username == null) {
			throw new IllegalArgumentException("username은 필수입니다.");
		}

		String normalized = username.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("username은 필수입니다.");
		}

		return normalized;
	}

	private void validatePassword(String rawPassword) {
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalArgumentException("password는 필수입니다.");
		}
	}
}
