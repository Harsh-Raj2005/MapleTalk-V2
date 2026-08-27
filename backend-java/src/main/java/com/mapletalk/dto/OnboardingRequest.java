package com.mapletalk.dto;

public record OnboardingRequest(
		String fullName,
		String bio,
		String nativeLanguage,
		String learningLanguage,
		String location) {
}
