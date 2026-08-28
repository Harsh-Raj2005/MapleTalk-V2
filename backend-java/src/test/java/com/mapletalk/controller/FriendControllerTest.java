package com.mapletalk.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.mapletalk.entity.User;
import com.mapletalk.repository.UserRepository;
import com.mapletalk.security.JwtService;

// Full Spring Security filter chain + real business logic, against the H2
// test database. No manually-inserted MySQL data.
@SpringBootTest
@AutoConfigureMockMvc
class FriendControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	private User user(String email) {
		return userRepository.saveAndFlush(new User("Fixture " + email, email, "irrelevant-hash"));
	}

	private String tokenFor(User user) {
		return jwtService.generateToken(user.getEmail());
	}

	private static Long extractId(String json) {
		Number id = JsonPath.read(json, "$.id");
		return id.longValue();
	}

	@Test
	void sendRequestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/friends/requests/1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void friendEndpointsWithMalformedTokenAreUnauthorized() throws Exception {
		mockMvc.perform(get("/api/friends").header("Authorization", "Bearer not-a-real-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void fullSendAcceptFlowWorksEndToEndOverHttp() throws Exception {
		User alice = user("http.alice@example.com");
		User bob = user("http.bob@example.com");

		String requestJson = mockMvc.perform(post("/api/friends/requests/" + bob.getId())
						.header("Authorization", "Bearer " + tokenFor(alice)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andReturn().getResponse().getContentAsString();

		Long requestId = extractId(requestJson);

		mockMvc.perform(get("/api/friends/requests").header("Authorization", "Bearer " + tokenFor(bob)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].sender.email").value("http.alice@example.com"));

		mockMvc.perform(put("/api/friends/requests/" + requestId + "/accept")
						.header("Authorization", "Bearer " + tokenFor(bob)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"));

		mockMvc.perform(get("/api/friends").header("Authorization", "Bearer " + tokenFor(alice)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("http.bob@example.com"));

		mockMvc.perform(get("/api/friends").header("Authorization", "Bearer " + tokenFor(bob)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("http.alice@example.com"));
	}

	@Test
	void rejectFlowWorksOverHttpAndDoesNotCreateFriendship() throws Exception {
		User carol = user("http.carol@example.com");
		User dave = user("http.dave@example.com");

		String requestJson = mockMvc.perform(post("/api/friends/requests/" + dave.getId())
						.header("Authorization", "Bearer " + tokenFor(carol)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Long requestId = extractId(requestJson);

		mockMvc.perform(put("/api/friends/requests/" + requestId + "/reject")
						.header("Authorization", "Bearer " + tokenFor(dave)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		mockMvc.perform(get("/api/friends").header("Authorization", "Bearer " + tokenFor(carol)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

}
