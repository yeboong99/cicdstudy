package com.neulboong.forcicd.calculator.history.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neulboong.forcicd.calculator.history.domain.CalculationHistory;

public interface CalculationHistoryRepository extends JpaRepository<CalculationHistory, Long> {

	List<CalculationHistory> findByUserId(Long userId, Pageable pageable);

	Optional<CalculationHistory> findByHistoryIdAndUserId(Long historyId, Long userId);
}
