package com.mapletalk.dto;

import java.time.Instant;

import com.mapletalk.entity.FriendRequest;
import com.mapletalk.entity.FriendRequestStatus;

public record FriendRequestResponse(
		Long id,
		UserResponse sender,
		UserResponse recipient,
		FriendRequestStatus status,
		Instant createdAt) {

	public static FriendRequestResponse from(FriendRequest request) {
		return new FriendRequestResponse(
				request.getId(),
				UserResponse.from(request.getSender()),
				UserResponse.from(request.getRecipient()),
				request.getStatus(),
				request.getCreatedAt());
	}

}
