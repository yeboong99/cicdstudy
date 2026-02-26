package com.neulboong.forcicd.calculator.history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.neulboong.forcicd.calculator.domain.Operator;
import com.neulboong.forcicd.calculator.history.domain.CalculationHistory;
import com.neulboong.forcicd.calculator.history.dto.CalculationCreateRequest;
import com.neulboong.forcicd.calculator.history.dto.CalculationHistoryItemResponse;
import com.neulboong.forcicd.calculator.history.dto.CalculationHistoryListResponse;
import com.neulboong.forcicd.calculator.history.exception.CalculationHistoryNotFoundException;
import com.neulboong.forcicd.calculator.history.exception.CalculationValidationException;
import com.neulboong.forcicd.calculator.history.repository.CalculationHistoryRepository;
import com.neulboong.forcicd.calculator.service.CalculatorService;

@ExtendWith(MockitoExtension.class)
class CalculationHistoryServiceTests {

	@Mock
	private CalculatorService calculatorService;

	@Mock
	private CalculationHistoryRepository calculationHistoryRepository;

	@InjectMocks
	private CalculationHistoryService calculationHistoryService;

	@Test
	@DisplayName("계산 실행 후 기록을 저장하고 응답을 반환한다")
	void calculateAndSave() {
		CalculationCreateRequest request = new CalculationCreateRequest(12, 5, "multiply");
		given(calculatorService.calculate(12, 5, Operator.MULTIPLY)).willReturn("60");
		given(calculationHistoryRepository.save(any(CalculationHistory.class))).willAnswer(invocation -> {
			CalculationHistory history = invocation.getArgument(0);
			ReflectionTestUtils.setField(history, "historyId", 101L);
			ReflectionTestUtils.setField(history, "createdAt", Instant.parse("2026-02-26T04:30:00Z"));
			return history;
		});

		CalculationHistoryItemResponse response = calculationHistoryService.calculateAndSave(1L, request);

		assertThat(response.historyId()).isEqualTo(101L);
		assertThat(response.firstNumber()).isEqualTo(12);
		assertThat(response.secondNumber()).isEqualTo(5);
		assertThat(response.operator()).isEqualTo("multiply");
		assertThat(response.result()).isEqualTo("60");
		assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-02-26T04:30:00Z"));

		ArgumentCaptor<CalculationHistory> captor = ArgumentCaptor.forClass(CalculationHistory.class);
		verify(calculationHistoryRepository).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo(1L);
		assertThat(captor.getValue().getOperator()).isEqualTo("multiply");
	}

	@Test
	@DisplayName("history 조회는 기본 limit=20, createdAt desc 정렬로 수행한다")
	void getHistoryUsesDefaultLimitAndSort() {
		CalculationHistory newest = history(12L, 1L, 9, 3, "divide", "3", "2026-02-26T04:31:00Z");
		CalculationHistory older = history(11L, 1L, 10, 2, "add", "12", "2026-02-26T04:30:00Z");
		given(calculationHistoryRepository.findByUserId(eq(1L), any(Pageable.class))).willReturn(List.of(newest, older));

		CalculationHistoryListResponse response = calculationHistoryService.getHistory(1L, null);

		assertThat(response.count()).isEqualTo(2);
		assertThat(response.items()).hasSize(2);
		assertThat(response.items().get(0).historyId()).isEqualTo(12L);
		assertThat(response.items().get(1).historyId()).isEqualTo(11L);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(calculationHistoryRepository).findByUserId(eq(1L), pageableCaptor.capture());
		Pageable pageable = pageableCaptor.getValue();
		assertThat(pageable.getPageSize()).isEqualTo(20);
		Sort.Order createdAtOrder = pageable.getSort().getOrderFor("createdAt");
		assertThat(createdAtOrder).isNotNull();
		assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("limit 범위를 벗어나면 검증 에러를 반환한다")
	void getHistoryWithInvalidLimit() {
		assertThatThrownBy(() -> calculationHistoryService.getHistory(1L, 101))
			.isInstanceOf(CalculationValidationException.class)
			.hasMessage("limit는 1 이상 100 이하여야 합니다.");
	}

	@Test
	@DisplayName("본인 소유가 아닌 historyId는 조회할 수 없다")
	void getHistoryDetailNotFound() {
		given(calculationHistoryRepository.findByHistoryIdAndUserId(101L, 2L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> calculationHistoryService.getHistoryDetail(2L, 101L))
			.isInstanceOf(CalculationHistoryNotFoundException.class)
			.hasMessage("계산 기록을 찾을 수 없습니다.");
	}

	private CalculationHistory history(Long historyId, Long userId, int first, int second, String operator, String result, String createdAt) {
		CalculationHistory history = CalculationHistory.create(userId, first, second, operator, result);
		ReflectionTestUtils.setField(history, "historyId", historyId);
		ReflectionTestUtils.setField(history, "createdAt", Instant.parse(createdAt));
		return history;
	}
}
