package com.neulboong.forcicd.calculator.history.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neulboong.forcicd.calculator.history.auth.SessionUserIdResolver;
import com.neulboong.forcicd.calculator.history.dto.CalculationCreateRequest;
import com.neulboong.forcicd.calculator.history.dto.CalculationHistoryItemResponse;
import com.neulboong.forcicd.calculator.history.dto.CalculationHistoryListResponse;
import com.neulboong.forcicd.calculator.history.service.CalculationHistoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/calculations")
@Validated
public class CalculationHistoryController {

	private final SessionUserIdResolver sessionUserIdResolver;
	private final CalculationHistoryService calculationHistoryService;

	@PostMapping
	public ResponseEntity<CalculationHistoryItemResponse> create(
		@Valid @RequestBody CalculationCreateRequest request,
		HttpServletRequest httpRequest
	) {
		Long userId = sessionUserIdResolver.resolveRequiredUserId(httpRequest.getSession(false));
		CalculationHistoryItemResponse response = calculationHistoryService.calculateAndSave(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/history")
	public ResponseEntity<CalculationHistoryListResponse> getHistory(
		@RequestParam(required = false) Integer limit,
		HttpServletRequest httpRequest
	) {
		Long userId = sessionUserIdResolver.resolveRequiredUserId(httpRequest.getSession(false));
		CalculationHistoryListResponse response = calculationHistoryService.getHistory(userId, limit);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/history/{historyId}")
	public ResponseEntity<CalculationHistoryItemResponse> getHistoryDetail(
		@PathVariable Long historyId,
		HttpServletRequest httpRequest
	) {
		Long userId = sessionUserIdResolver.resolveRequiredUserId(httpRequest.getSession(false));
		CalculationHistoryItemResponse response = calculationHistoryService.getHistoryDetail(userId, historyId);
		return ResponseEntity.ok(response);
	}
}
