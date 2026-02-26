package com.neulboong.forcicd.calculator.history.auth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.neulboong.forcicd.calculator.history.exception.AuthenticationRequiredException;

import jakarta.servlet.http.HttpSession;

@Component
public class SessionUserIdResolver {

	private static final List<String> USER_ID_ATTRIBUTE_KEYS = List.of("LOGIN_USER_ID", "loginUserId", "userId", "USER_ID");
	private static final List<String> USER_OBJECT_ATTRIBUTE_KEYS = List.of("LOGIN_USER", "loginUser", "user", "member");

	public Long resolveRequiredUserId(HttpSession session) {
		if (session == null) {
			throw new AuthenticationRequiredException();
		}

		for (String key : USER_ID_ATTRIBUTE_KEYS) {
			Long userId = toLong(session.getAttribute(key));
			if (userId != null) {
				return userId;
			}
		}

		for (String key : USER_OBJECT_ATTRIBUTE_KEYS) {
			Object user = session.getAttribute(key);
			Long userId = extractFromObject(user);
			if (userId != null) {
				return userId;
			}
		}

		throw new AuthenticationRequiredException();
	}

	private Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Long number) {
			return number;
		}
		if (value instanceof Integer number) {
			return number.longValue();
		}
		if (value instanceof String text) {
			try {
				return Long.parseLong(text);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	@SuppressWarnings("java:S3011")
	private Long extractFromObject(Object user) {
		if (user == null) {
			return null;
		}
		if (user instanceof Map<?, ?> map) {
			Long userId = toLong(map.get("userId"));
			if (userId != null) {
				return userId;
			}
			return toLong(map.get("id"));
		}

		for (String methodName : List.of("getUserId", "userId", "getId", "id")) {
			try {
				Method method = user.getClass().getMethod(methodName);
				method.setAccessible(true);
				Long userId = toLong(method.invoke(user));
				if (userId != null) {
					return userId;
				}
			} catch (ReflectiveOperationException ignored) {
				// no-op
			}
		}

		for (String fieldName : List.of("userId", "id")) {
			try {
				Field field = user.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				Long userId = toLong(field.get(user));
				if (userId != null) {
					return userId;
				}
			} catch (ReflectiveOperationException ignored) {
				// no-op
			}
		}

		return null;
	}
}
