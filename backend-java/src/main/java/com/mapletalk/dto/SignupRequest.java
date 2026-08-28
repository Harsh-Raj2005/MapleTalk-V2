package com.mapletalk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank(message = "fullName is required") String fullName,

		@NotBlank(message = "email is required")
		@Email(message = "must be a valid email") String email,

		@NotBlank(message = "password is required")
		@Size(min = 6, message = "password must be at least 6 characters long") String password) {
}
