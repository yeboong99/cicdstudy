package com.neulboong.forcicd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.neulboong.forcicd.auth.domain.Member;
import com.neulboong.forcicd.auth.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Auth 통합 테스트")
class AuthIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@BeforeEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	@Nested
	@DisplayName("POST /api/v1/auth/signup")
	class SignupTests {

		@Test
		@DisplayName("회원가입 성공 시 201과 사용자 정보를 반환하고 비밀번호는 BCrypt 해시로 저장한다")
		void signupSuccess() throws Exception {
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.username").value("alice"));

			Member member = memberRepository.findByUsername("alice").orElseThrow();
			assertThat(member.getPasswordHash()).startsWith("$2");
			assertThat(member.getPasswordHash()).isNotEqualTo("password123");
		}

		@Test
		@DisplayName("username 중복 시 409 공통 에러 포맷을 반환한다")
		void signupDuplicateUsername() throws Exception {
			memberRepository.save(new Member("alice", "$2a$10$alreadyHashedPasswordValue"));

			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"));
		}
	}

	@Nested
	@DisplayName("POST /api/v1/auth/login")
	class LoginTests {

		@Test
		@DisplayName("로그인 성공 시 세션이 생성되고 사용자 정보를 반환한다")
		void loginSuccess() throws Exception {
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isCreated());

			MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice"))
				.andReturn();

			assertThat(loginResult.getRequest().getSession(false)).isNotNull();
		}

		@Test
		@DisplayName("비밀번호 불일치 시 401 공통 에러 포맷을 반환한다")
		void loginFail() throws Exception {
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isCreated());

			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "wrong-password"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"));
		}
	}

	@Nested
	@DisplayName("GET /api/v1/auth/me, POST /api/v1/auth/logout")
	class SessionFlowTests {

		@Test
		@DisplayName("로그인 후 /me 호출 시 현재 사용자 정보를 반환한다")
		void meAfterLogin() throws Exception {
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isCreated());

			MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();

			MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
			assertThat(session).isNotNull();

			mockMvc.perform(get("/api/v1/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice"));
		}

		@Test
		@DisplayName("세션 없이 /me 호출 시 401 공통 에러 포맷을 반환한다")
		void meWithoutSession() throws Exception {
			mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"));
		}

		@Test
		@DisplayName("로그인 후 /logout 호출 시 200과 메시지를 반환한다")
		void logoutSuccess() throws Exception {
			mockMvc.perform(post("/api/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isCreated());

			MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "username": "alice",
						  "password": "password123"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();

			MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

			mockMvc.perform(post("/api/v1/auth/logout").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
		}

		@Test
		@DisplayName("세션 없이 /logout 호출 시 401 공통 에러 포맷을 반환한다")
		void logoutWithoutSession() throws Exception {
			mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"));
		}
	}
}
