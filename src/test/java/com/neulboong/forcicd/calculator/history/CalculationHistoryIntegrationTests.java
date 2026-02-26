package com.neulboong.forcicd.calculator.history;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.neulboong.forcicd.calculator.history.repository.CalculationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Calculation History 통합 테스트")
class CalculationHistoryIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CalculationHistoryRepository calculationHistoryRepository;

	@BeforeEach
	void setUp() {
		calculationHistoryRepository.deleteAll();
	}

	@Test
	@DisplayName("미인증 사용자의 POST /api/v1/calculations 요청은 401")
	void createHistoryWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/calculations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody(12, 5, "multiply")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.error").value("Unauthorized"))
			.andExpect(jsonPath("$.code").value("AUTH_002"));
	}

	@Test
	@DisplayName("로그인 사용자는 계산 기록을 생성하고 최신순으로 조회할 수 있다")
	void createAndReadHistory() throws Exception {
		MockHttpSession session = loginSession(1L);

		createHistory(session, 10, 2, "add");
		createHistory(session, 8, 3, "multiply");

		mockMvc.perform(get("/api/v1/calculations/history")
				.param("limit", "20")
				.session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(2))
			.andExpect(jsonPath("$.items[0].operator").value("multiply"))
			.andExpect(jsonPath("$.items[1].operator").value("add"));
	}

	@Test
	@DisplayName("본인 소유가 아닌 계산 기록 단건 조회는 404")
	void getHistoryDetailOfAnotherUser() throws Exception {
		MockHttpSession ownerSession = loginSession(1L);
		long historyId = createHistory(ownerSession, 12, 3, "divide");

		MockHttpSession anotherSession = loginSession(2L);
		mockMvc.perform(get("/api/v1/calculations/history/{historyId}", historyId)
				.session(anotherSession))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.code").value("CALC_404"));
	}

	@Test
	@DisplayName("로그인 사용자는 본인 계산 기록 단건을 조회할 수 있다")
	void getHistoryDetail() throws Exception {
		MockHttpSession session = loginSession(1L);
		long historyId = createHistory(session, 15, 5, "subtract");

		mockMvc.perform(get("/api/v1/calculations/history/{historyId}", historyId)
				.session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.historyId").value(historyId))
			.andExpect(jsonPath("$.firstNumber").value(15))
			.andExpect(jsonPath("$.secondNumber").value(5))
			.andExpect(jsonPath("$.operator").value("subtract"));
	}

	private MockHttpSession loginSession(Long userId) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("LOGIN_USER_ID", userId);
		return session;
	}

	private long createHistory(MockHttpSession session, int firstNumber, int secondNumber, String operator) throws Exception {
		mockMvc.perform(post("/api/v1/calculations")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content(requestBody(firstNumber, secondNumber, operator)))
			.andExpect(status().isCreated());

		return calculationHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "historyId"))
			.get(0)
			.getHistoryId();
	}

	private String requestBody(int firstNumber, int secondNumber, String operator) {
		return """
			{
			  "firstNumber": %d,
			  "secondNumber": %d,
			  "operator": "%s"
			}
			""".formatted(firstNumber, secondNumber, operator);
	}
}
