package com.mapletalk.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mapletalk.dto.OnboardingRequest;
import com.mapletalk.dto.UserResponse;
import com.mapletalk.service.UserService;

/**
 * Current-user endpoints. Identity always comes from the SecurityContext
 * (the JWT subject/email), never from a client-supplied id.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public UserResponse me(Authentication authentication) {
		return userService.getCurrentUser(authentication.getName());
	}

	@PutMapping("/onboarding")
	public UserResponse onboarding(Authentication authentication, @RequestBody OnboardingRequest request) {
		return userService.updateOnboarding(authentication.getName(), request);
	}

	@GetMapping("/recommended")
	public List<UserResponse> recommended(Authentication authentication) {
		return userService.getRecommendedUsers(authentication.getName());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleMissingUser(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
	}

}
