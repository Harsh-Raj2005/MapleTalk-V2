package com.mapletalk.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardingRequest(
		@NotBlank(message = "fullName is required") String fullName,
		@NotBlank(message = "bio is required") String bio,
		@NotBlank(message = "nativeLanguage is required") String nativeLanguage,
		@NotBlank(message = "learningLanguage is required") String learningLanguage,
		@NotBlank(message = "location is required") String location) {
}
