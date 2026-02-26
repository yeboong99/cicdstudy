package com.neulboong.forcicd.calculator.history.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.neulboong.forcicd.calculator.domain.Operator;
import com.neulboong.forcicd.calculator.history.domain.CalculationHistory;
import com.neulboong.forcicd.calculator.history.dto.CalculationCreateRequest;
import com.neulboong.forcicd.calculator.history.dto.CalculationHistoryItemResponse;
import com.neulboong.forcicd.calculator.history.dto.CalculationHistoryListResponse;
import com.neulboong.forcicd.calculator.history.exception.CalculationHistoryNotFoundException;
import com.neulboong.forcicd.calculator.history.exception.CalculationValidationException;
import com.neulboong.forcicd.calculator.history.repository.CalculationHistoryRepository;
import com.neulboong.forcicd.calculator.service.CalculatorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalculationHistoryService {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 100;

	private final CalculatorService calculatorService;
	private final CalculationHistoryRepository calculationHistoryRepository;

	public CalculationHistoryItemResponse calculateAndSave(Long userId, CalculationCreateRequest request) {
		Operator operator = Operator.from(request.operator());
		String result = calculatorService.calculate(request.firstNumber(), request.secondNumber(), operator);
		CalculationHistory saved = calculationHistoryRepository.save(
			CalculationHistory.create(userId, request.firstNumber(), request.secondNumber(), operator.getValue(), result)
		);
		return CalculationHistoryItemResponse.from(saved);
	}

	public CalculationHistoryListResponse getHistory(Long userId, Integer rawLimit) {
		int limit = normalizeLimit(rawLimit);
		PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("historyId")));
		List<CalculationHistoryItemResponse> items = calculationHistoryRepository.findByUserId(userId, pageRequest)
			.stream()
			.map(CalculationHistoryItemResponse::from)
			.toList();
		return new CalculationHistoryListResponse(items.size(), items);
	}

	public CalculationHistoryItemResponse getHistoryDetail(Long userId, Long historyId) {
		CalculationHistory history = calculationHistoryRepository.findByHistoryIdAndUserId(historyId, userId)
			.orElseThrow(CalculationHistoryNotFoundException::new);
		return CalculationHistoryItemResponse.from(history);
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new CalculationValidationException("limit는 1 이상 100 이하여야 합니다.");
		}
		return limit;
	}
}
