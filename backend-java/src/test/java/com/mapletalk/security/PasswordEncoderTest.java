package com.mapletalk.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Plain unit test — no Spring context needed to exercise a BCryptPasswordEncoder.
class PasswordEncoderTest {

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	void encodedPasswordIsNotEqualToRawPassword() {
		String raw = "correct-horse-battery-staple";

		String encoded = passwordEncoder.encode(raw);

		assertThat(encoded).isNotEqualTo(raw);
	}

	@Test
	void matchesReturnsTrueForCorrectRawPassword() {
		String raw = "correct-horse-battery-staple";
		String encoded = passwordEncoder.encode(raw);

		assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
	}

	@Test
	void matchesReturnsFalseForIncorrectRawPassword() {
		String encoded = passwordEncoder.encode("correct-horse-battery-staple");

		assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
	}

}
