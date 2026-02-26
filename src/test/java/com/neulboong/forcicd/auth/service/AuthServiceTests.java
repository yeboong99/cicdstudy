package com.neulboong.forcicd.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.neulboong.forcicd.auth.domain.Member;
import com.neulboong.forcicd.auth.exception.AuthenticationFailedException;
import com.neulboong.forcicd.auth.exception.DuplicateUsernameException;
import com.neulboong.forcicd.auth.exception.UnauthorizedException;
import com.neulboong.forcicd.auth.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTests {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthService authService;

	@Nested
	@DisplayName("회원가입")
	class SignupTests {

		@Test
		@DisplayName("사용 가능한 username이면 비밀번호를 해시해 저장한다")
		void signupSuccess() {
			given(memberRepository.findByUsername("alice")).willReturn(Optional.empty());
			given(passwordEncoder.encode("password123")).willReturn("hashed-password");
			given(memberRepository.save(any(Member.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			Member created = authService.signup("alice", "password123");

			ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
			verify(memberRepository).save(captor.capture());

			assertThat(captor.getValue().getUsername()).isEqualTo("alice");
			assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
			assertThat(created.getUsername()).isEqualTo("alice");
		}

		@Test
		@DisplayName("이미 존재하는 username이면 409 예외를 던진다")
		void signupDuplicateUsername() {
			given(memberRepository.findByUsername("alice"))
				.willReturn(Optional.of(new Member("alice", "hashed")));

			assertThatThrownBy(() -> authService.signup("alice", "password123"))
				.isInstanceOf(DuplicateUsernameException.class);
		}
	}

	@Nested
	@DisplayName("로그인 인증")
	class AuthenticateTests {

		@Test
		@DisplayName("username/password가 일치하면 회원을 반환한다")
		void authenticateSuccess() {
			Member member = new Member("alice", "hashed");
			given(memberRepository.findByUsername("alice")).willReturn(Optional.of(member));
			given(passwordEncoder.matches("password123", "hashed")).willReturn(true);

			Member authenticated = authService.authenticate("alice", "password123");

			assertThat(authenticated).isEqualTo(member);
		}

		@Test
		@DisplayName("비밀번호가 다르면 인증 실패 예외를 던진다")
		void authenticateWrongPassword() {
			Member member = new Member("alice", "hashed");
			given(memberRepository.findByUsername("alice")).willReturn(Optional.of(member));
			given(passwordEncoder.matches("wrong-password", "hashed")).willReturn(false);

			assertThatThrownBy(() -> authService.authenticate("alice", "wrong-password"))
				.isInstanceOf(AuthenticationFailedException.class);
		}

		@Test
		@DisplayName("존재하지 않는 username이면 인증 실패 예외를 던진다")
		void authenticateUnknownUsername() {
			given(memberRepository.findByUsername("unknown")).willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.authenticate("unknown", "password123"))
				.isInstanceOf(AuthenticationFailedException.class);
		}
	}

	@Nested
	@DisplayName("세션 사용자 조회")
	class FindMemberTests {

		@Test
		@DisplayName("회원 ID가 없으면 인증 필요 예외를 던진다")
		void getMemberByIdUnauthorized() {
			given(memberRepository.findById(99L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.getMemberById(99L))
				.isInstanceOf(UnauthorizedException.class);
		}
	}
}
