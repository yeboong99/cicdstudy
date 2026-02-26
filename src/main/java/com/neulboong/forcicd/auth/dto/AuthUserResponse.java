package com.neulboong.forcicd.auth.dto;

import com.neulboong.forcicd.auth.domain.Member;

public record AuthUserResponse(Long id, String username) {

	public static AuthUserResponse from(Member member) {
		return new AuthUserResponse(member.getId(), member.getUsername());
	}
}
