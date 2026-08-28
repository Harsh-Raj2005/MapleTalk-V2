package com.mapletalk.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mapletalk.dto.FriendRequestResponse;
import com.mapletalk.dto.UserResponse;
import com.mapletalk.service.FriendService;

/**
 * Friend-request and friendship endpoints. A path variable may identify the
 * target user or the request being acted on, but the acting user always
 * comes from the authenticated JWT identity, never the client.
 */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

	private final FriendService friendService;

	public FriendController(FriendService friendService) {
		this.friendService = friendService;
	}

	@PostMapping("/requests/{userId}")
	public ResponseEntity<FriendRequestResponse> sendRequest(Authentication authentication, @PathVariable Long userId) {
		FriendRequestResponse response = friendService.sendRequest(authentication.getName(), userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/requests")
	public List<FriendRequestResponse> incomingRequests(Authentication authentication) {
		return friendService.getIncomingRequests(authentication.getName());
	}

	@PutMapping("/requests/{requestId}/accept")
	public FriendRequestResponse acceptRequest(Authentication authentication, @PathVariable Long requestId) {
		return friendService.acceptRequest(authentication.getName(), requestId);
	}

	@PutMapping("/requests/{requestId}/reject")
	public FriendRequestResponse rejectRequest(Authentication authentication, @PathVariable Long requestId) {
		return friendService.rejectRequest(authentication.getName(), requestId);
	}

	@GetMapping
	public List<UserResponse> friends(Authentication authentication) {
		return friendService.getFriends(authentication.getName());
	}

}
