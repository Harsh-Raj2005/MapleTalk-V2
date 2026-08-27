package com.mapletalk.service;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapletalk.dto.AuthResponse;
import com.mapletalk.dto.LoginRequest;
import com.mapletalk.dto.SignupRequest;
import com.mapletalk.dto.UserResponse;
import com.mapletalk.entity.User;
import com.mapletalk.exception.EmailAlreadyExistsException;
import com.mapletalk.exception.InvalidCredentialsException;
import com.mapletalk.repository.UserRepository;
import com.mapletalk.security.JwtService;

/**
 * Signup and login only. Deliberately has no knowledge of HTTP — it throws
 * plain exceptions ({@link EmailAlreadyExistsException},
 * {@link InvalidCredentialsException}, {@link IllegalArgumentException}) and
 * leaves status-code mapping to the controller.
 */
@Service
public class AuthService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final int MIN_PASSWORD_LENGTH = 6;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		String fullName = blankToNull(request.fullName());
		String email = blankToNull(request.email());
		String password = request.password();

		if (fullName == null || email == null || password == null || password.isBlank()) {
			throw new IllegalArgumentException("fullName, email and password are all required");
		}
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
		}
		if (!EMAIL_PATTERN.matcher(email).matches()) {
			throw new IllegalArgumentException("Invalid email format");
		}
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException("An account with this email already exists");
		}

		User user = new User(fullName, email, passwordEncoder.encode(password));
		user.setProfilePic(randomAvatarUrl());

		User saved = userRepository.save(user);
		String token = jwtService.generateToken(saved.getEmail());

		return new AuthResponse(token, UserResponse.from(saved));
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = blankToNull(request.email());
		String password = request.password();

		if (email == null || password == null || password.isBlank()) {
			throw new IllegalArgumentException("email and password are both required");
		}

		// Same generic failure for "no such user" and "wrong password" —
		// never reveal which one it was.
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		String token = jwtService.generateToken(user.getEmail());
		return new AuthResponse(token, UserResponse.from(user));
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	private static String randomAvatarUrl() {
		int idx = (int) (Math.random() * 100) + 1;
		return "https://avatar.iran.liara.run/public/" + idx + ".png";
	}

}
