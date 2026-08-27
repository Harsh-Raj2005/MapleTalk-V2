package com.mapletalk.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// Plain unit test — JwtService is a simple POJO here, no Spring context needed.
class JwtServiceTest {

	private static final String SECRET = "unit-test-jwt-secret-key-at-least-32-bytes-long";

	private final JwtService jwtService = new JwtService(SECRET, 60_000);

	@Test
	void generatedTokenRoundTripsToTheSameSubject() {
		String token = jwtService.generateToken("user@example.com");

		assertThat(jwtService.validateToken(token)).isTrue();
		assertThat(jwtService.extractSubject(token)).isEqualTo("user@example.com");
	}

	@Test
	void malformedTokenIsInvalid() {
		assertThat(jwtService.validateToken("not-a-real-jwt")).isFalse();
	}

	@Test
	void tokenSignedWithADifferentSecretIsInvalid() {
		JwtService otherService = new JwtService("a-completely-different-jwt-secret-key-of-32b+", 60_000);
		String token = otherService.generateToken("user@example.com");

		assertThat(jwtService.validateToken(token)).isFalse();
	}

	@Test
	void expiredTokenIsInvalid() {
		JwtService alreadyExpiredService = new JwtService(SECRET, -1_000);
		String token = alreadyExpiredService.generateToken("user@example.com");

		assertThat(jwtService.validateToken(token)).isFalse();
	}

}
