package com.mapletalk.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.mapletalk.entity.User;
import com.mapletalk.repository.UserRepository;
import com.mapletalk.security.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Full Spring Security filter chain + real business logic, against the H2
// test database.
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

	private static final String TEST_STREAM_SECRET = "test-only-dev-stream-secret-not-for-production-use-0123456789";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Test
	void tokenEndpointWithoutJwtIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/chat/token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenEndpointWithMalformedJwtIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/chat/token").header("Authorization", "Bearer not-a-real-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenEndpointReturnsTokenForTheAuthenticatedUsersOwnId() throws Exception {
		User user = userRepository.saveAndFlush(new User("Chat User", "chat.user@example.com", "hash"));
		String jwt = jwtService.generateToken(user.getEmail());

		String responseJson = mockMvc.perform(get("/api/chat/token").header("Authorization", "Bearer " + jwt))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists())
				.andReturn().getResponse().getContentAsString();

		String streamToken = com.jayway.jsonpath.JsonPath.read(responseJson, "$.token");

		// Decode using the same (test) Stream secret the app is configured
		// with, to prove the token really identifies this user — not a
		// client-suppliable one, since the endpoint takes no id parameter.
		var claims = Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(TEST_STREAM_SECRET.getBytes()))
				.build()
				.parseSignedClaims(streamToken)
				.getPayload();

		assertThat(claims.get("user_id", String.class)).isEqualTo(String.valueOf(user.getId()));
	}

	@Test
	void responseNeverContainsTheStreamSecret() throws Exception {
		User user = userRepository.saveAndFlush(new User("Chat User 2", "chat.user2@example.com", "hash"));
		String jwt = jwtService.generateToken(user.getEmail());

		String responseJson = mockMvc.perform(get("/api/chat/token").header("Authorization", "Bearer " + jwt))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(responseJson).doesNotContain(TEST_STREAM_SECRET);
	}

}
