package com.mapletalk.dto;

import java.time.Instant;

import com.mapletalk.entity.User;

/**
 * Public-facing user projection. Deliberately excludes {@code password}
 * (password_hash) and any other authentication internals — this is the only
 * shape of a User that ever leaves the API.
 */
public record UserResponse(
		Long id,
		String fullName,
		String email,
		String profilePic,
		String bio,
		String nativeLanguage,
		String learningLanguage,
		String location,
		boolean isOnboarded,
		Instant createdAt,
		Instant updatedAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getProfilePic(),
				user.getBio(),
				user.getNativeLanguage(),
				user.getLearningLanguage(),
				user.getLocation(),
				user.isOnboarded(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

}
