package com.mapletalk.service;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final int MIN_PASSWORD_LENGTH = 6;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final StreamService streamService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			StreamService streamService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.streamService = streamService;
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
			log.warn("Signup rejected: email already registered [{}]", email);
			throw new EmailAlreadyExistsException("An account with this email already exists");
		}

		User user = new User(fullName, email, passwordEncoder.encode(password));
		user.setProfilePic(randomAvatarUrl());

		User saved = userRepository.save(user);
		String token = jwtService.generateToken(saved.getEmail());

		// So any future friend can open a chat with this user immediately,
		// even before they've ever opened the chat page themselves — Stream
		// requires channel members to already exist server-side.
		streamService.upsertUser(String.valueOf(saved.getId()), saved.getFullName(), saved.getProfilePic());

		log.info("User signed up successfully [userId={}]", saved.getId());
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
		// never reveal which one it was (the log message may distinguish
		// them for operators, but the thrown exception/response never does).
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> {
					log.warn("Login failed: no account for email [{}]", email);
					return new InvalidCredentialsException("Invalid email or password");
				});

		if (!passwordEncoder.matches(password, user.getPassword())) {
			log.warn("Login failed: incorrect password [userId={}]", user.getId());
			throw new InvalidCredentialsException("Invalid email or password");
		}

		String token = jwtService.generateToken(user.getEmail());
		log.info("User logged in successfully [userId={}]", user.getId());
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
