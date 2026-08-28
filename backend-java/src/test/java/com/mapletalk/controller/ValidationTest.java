package com.mapletalk.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.mapletalk.entity.User;
import com.mapletalk.repository.UserRepository;
import com.mapletalk.security.JwtService;

// Bean Validation only fires at the HTTP boundary (@Valid on @RequestBody),
// so these are MockMvc tests rather than direct service calls.
@SpringBootTest
@AutoConfigureMockMvc
class ValidationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Test
	void invalidSignupEmailReturnsConsistentValidationError() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Val User","email":"not-an-email","password":"validpass"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	void blankSignupFieldsAreRejected() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"","email":"","password":""}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.fullName").exists())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void shortSignupPasswordIsRejected() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Val User","email":"val.short@example.com","password":"abc"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void blankLoginFieldsAreRejected() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"","password":""}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void malformedRequestBodyReturnsConsistentBadRequest() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not valid json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void invalidOnboardingRequestIsRejected() throws Exception {
		User user = userRepository.saveAndFlush(new User("Val Onboard", "val.onboard@example.com", "hash"));
		String token = jwtService.generateToken(user.getEmail());

		mockMvc.perform(put("/api/users/onboarding")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"","bio":"","nativeLanguage":"","learningLanguage":"","location":""}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.bio").exists());
	}

	@Test
	void duplicateSignupReturnsConsistentConflictEnvelope() throws Exception {
		String body = """
				{"fullName":"Dup User","email":"val.dup@example.com","password":"validpass"}""";

		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void invalidLoginCredentialsRemainUnauthorizedWithConsistentEnvelope() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"no-such-user@example.com","password":"whatever1"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

}
