package com.mapletalk.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mapletalk.dto.StreamTokenResponse;
import com.mapletalk.dto.UserResponse;
import com.mapletalk.service.StreamService;
import com.mapletalk.service.UserService;

/**
 * Issues Stream Chat/Video tokens for the authenticated user only — there is
 * no way to request a token for a different user, since the identity always
 * comes from the SecurityContext, never a client-supplied id.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final UserService userService;
	private final StreamService streamService;

	public ChatController(UserService userService, StreamService streamService) {
		this.userService = userService;
		this.streamService = streamService;
	}

	@GetMapping("/token")
	public StreamTokenResponse getToken(Authentication authentication) {
		UserResponse currentUser = userService.getCurrentUser(authentication.getName());
		String token = streamService.generateToken(String.valueOf(currentUser.id()));
		return new StreamTokenResponse(token);
	}

}
