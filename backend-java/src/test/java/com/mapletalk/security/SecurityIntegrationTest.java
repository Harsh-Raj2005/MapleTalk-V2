package com.mapletalk.security;

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

// Full Spring Security filter chain exercised through MockMvc, against the
// H2 test database — not mocked, so this proves the real JwtAuthFilter and
// SecurityConfig behavior, not a stand-in for them.
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void healthEndpointIsPublicWithoutToken() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}

	@Test
	void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/test/protected"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithMalformedTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/test/protected")
						.header("Authorization", "Bearer not-a-real-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithBadSignatureTokenIsUnauthorized() throws Exception {
		JwtService attackerService = new JwtService("attacker-controlled-secret-key-of-32-bytes-min", 60_000);
		String forgedToken = attackerService.generateToken("someone@example.com");

		mockMvc.perform(get("/api/test/protected")
						.header("Authorization", "Bearer " + forgedToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithValidTokenIsAuthorizedAndIdentifiesUser() throws Exception {
		User user = userRepository.saveAndFlush(
				new User("Security Test User", "security-test@example.com", "irrelevant-hash"));
		String token = jwtService.generateToken(user.getEmail());

		mockMvc.perform(get("/api/test/protected")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true))
				.andExpect(jsonPath("$.user").value("security-test@example.com"));
	}

}
