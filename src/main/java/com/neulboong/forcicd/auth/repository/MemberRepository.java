package com.neulboong.forcicd.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neulboong.forcicd.auth.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByUsername(String username);
}
