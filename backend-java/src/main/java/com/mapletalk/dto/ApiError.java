package com.mapletalk.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One consistent error shape for every non-2xx API response. {@code errors}
 * is only populated for field-level validation failures and omitted
 * otherwise.
 */
public record ApiError(
		int status,
		String message,
		@JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> errors,
		Instant timestamp) {

	public ApiError(int status, String message) {
		this(status, message, null, Instant.now());
	}

	public ApiError(int status, String message, Map<String, String> errors) {
		this(status, message, errors, Instant.now());
	}

}
