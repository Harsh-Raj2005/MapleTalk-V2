package com.mapletalk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mapletalk.dto.AuthResponse;
import com.mapletalk.dto.LoginRequest;
import com.mapletalk.dto.SignupRequest;
import com.mapletalk.entity.User;
import com.mapletalk.exception.EmailAlreadyExistsException;
import com.mapletalk.exception.InvalidCredentialsException;
import com.mapletalk.repository.UserRepository;

// Full Spring context against the H2 test database — real persistence and
// real BCrypt/JWT beans, not mocks.
@SpringBootTest
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void signupCreatesUserWithBCryptHashedPassword() {
		AuthResponse response = authService.signup(
				new SignupRequest("Ada Lovelace", "ada.signup@example.com", "correct-horse"));

		assertThat(response.token()).isNotBlank();
		assertThat(response.user().email()).isEqualTo("ada.signup@example.com");

		User saved = userRepository.findByEmail("ada.signup@example.com").orElseThrow();
		assertThat(saved.getPassword()).isNotEqualTo("correct-horse");
		assertThat(saved.getPassword()).startsWith("$2"); // BCrypt hash prefix
	}

	@Test
	void signupRejectsDuplicateEmail() {
		authService.signup(new SignupRequest("First", "dup.signup@example.com", "password1"));

		assertThatThrownBy(() -> authService.signup(
				new SignupRequest("Second", "dup.signup@example.com", "password2")))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}

	@Test
	void signupRejectsShortPassword() {
		assertThatThrownBy(() -> authService.signup(
				new SignupRequest("Short Pw", "shortpw@example.com", "abc")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void loginWithCorrectCredentialsReturnsToken() {
		authService.signup(new SignupRequest("Login User", "login.ok@example.com", "correct-password"));

		AuthResponse response = authService.login(new LoginRequest("login.ok@example.com", "correct-password"));

		assertThat(response.token()).isNotBlank();
		assertThat(response.user().email()).isEqualTo("login.ok@example.com");
	}

	@Test
	void loginWithWrongPasswordFails() {
		authService.signup(new SignupRequest("Login User 2", "login.wrong@example.com", "correct-password"));

		assertThatThrownBy(() -> authService.login(new LoginRequest("login.wrong@example.com", "wrong-password")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void loginWithUnknownEmailFailsWithSameGenericError() {
		assertThatThrownBy(() -> authService.login(new LoginRequest("no-such-user@example.com", "whatever")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

}
