package com.mapletalk.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mapletalk.dto.ApiError;

// Plain unit test — no Spring context needed to exercise the handler methods.
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void unexpectedExceptionNeverLeaksInternalDetails() {
		Exception internalFailure = new RuntimeException("Connection refused at jdbc:mysql://internal-host:3306");

		ResponseEntity<ApiError> response = handler.handleUnexpected(internalFailure);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
		assertThat(response.getBody().message()).doesNotContain("jdbc:mysql", "internal-host", "RuntimeException");
	}

	@Test
	void notFoundHandlerUsesGenericMessageForVanishedAuthenticatedUser() {
		ResponseEntity<ApiError> response = handler.handleNotFound(
				new IllegalStateException("Authenticated user no longer exists: someone@example.com"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		// The IllegalStateException message (which contains an email) must
		// never reach the client — only the generic "User not found".
		assertThat(response.getBody().message()).isEqualTo("User not found");
	}

}
