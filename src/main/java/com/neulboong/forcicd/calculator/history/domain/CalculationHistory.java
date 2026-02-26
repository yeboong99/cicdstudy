package com.neulboong.forcicd.calculator.history.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "calculation_histories",
	indexes = {
		@Index(name = "idx_history_user_created_at", columnList = "user_id, created_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalculationHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long historyId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "first_number", nullable = false)
	private int firstNumber;

	@Column(name = "second_number", nullable = false)
	private int secondNumber;

	@Column(nullable = false, length = 20)
	private String operator;

	@Column(nullable = false, length = 100)
	private String result;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private CalculationHistory(Long userId, int firstNumber, int secondNumber, String operator, String result) {
		this.userId = userId;
		this.firstNumber = firstNumber;
		this.secondNumber = secondNumber;
		this.operator = operator;
		this.result = result;
	}

	public static CalculationHistory create(Long userId, int firstNumber, int secondNumber, String operator, String result) {
		return new CalculationHistory(userId, firstNumber, secondNumber, operator, result);
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
