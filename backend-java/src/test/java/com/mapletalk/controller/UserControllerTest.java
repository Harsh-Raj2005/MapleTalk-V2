package com.mapletalk.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.mapletalk.entity.Friendship;
import com.mapletalk.entity.User;
import com.mapletalk.repository.FriendshipRepository;
import com.mapletalk.repository.UserRepository;
import com.mapletalk.security.JwtService;

// Full Spring Security filter chain + real business logic, against the H2
// test database. No manually-inserted MySQL data — every fixture user is
// created fresh, in-test, so the suite is deterministic.
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FriendshipRepository friendshipRepository;

	@Autowired
	private JwtService jwtService;

	private User createOnboardedUser(String email) {
		User user = new User("Fixture User", email, "irrelevant-hash");
		user.setOnboarded(true);
		return userRepository.saveAndFlush(user);
	}

	private String tokenFor(User user) {
		return jwtService.generateToken(user.getEmail());
	}

	@Test
	void meWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meWithValidTokenReturnsCorrectUser() throws Exception {
		User user = createOnboardedUser("me.test@example.com");

		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("me.test@example.com"))
				.andExpect(jsonPath("$.id").value(user.getId()));
	}

	@Test
	void onboardingUpdatesAndPersistsOnlyTheCallersOwnRecord() throws Exception {
		User caller = createOnboardedUser("onboard.caller@example.com");
		User other = createOnboardedUser("onboard.other@example.com");

		String body = """
				{
					"fullName": "Updated Name",
					"bio": "Updated bio",
					"nativeLanguage": "English",
					"learningLanguage": "Spanish",
					"location": "Remote"
				}
				""";

		mockMvc.perform(put("/api/users/onboarding")
						.header("Authorization", "Bearer " + tokenFor(caller))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Updated Name"))
				.andExpect(jsonPath("$.isOnboarded").value(true));

		User reloadedCaller = userRepository.findById(caller.getId()).orElseThrow();
		assertThat(reloadedCaller.getFullName()).isEqualTo("Updated Name");
		assertThat(reloadedCaller.getBio()).isEqualTo("Updated bio");

		// The other user must be completely unaffected — onboarding can only
		// ever touch the authenticated caller's own row.
		User reloadedOther = userRepository.findById(other.getId()).orElseThrow();
		assertThat(reloadedOther.getFullName()).isEqualTo("Fixture User");
	}

	@Test
	void recommendedExcludesSelfAndExistingFriendsButIncludesEligibleUsers() throws Exception {
		User currentUser = createOnboardedUser("rec.current@example.com");
		User existingFriend = createOnboardedUser("rec.friend@example.com");
		User eligibleStranger = createOnboardedUser("rec.stranger@example.com");
		User notOnboarded = new User("Not Onboarded", "rec.notonboarded@example.com", "hash");
		userRepository.saveAndFlush(notOnboarded);

		friendshipRepository.saveAndFlush(Friendship.between(currentUser, existingFriend));

		mockMvc.perform(get("/api/users/recommended")
						.header("Authorization", "Bearer " + tokenFor(currentUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.email == 'rec.current@example.com')]").isEmpty())
				.andExpect(jsonPath("$[?(@.email == 'rec.friend@example.com')]").isEmpty())
				.andExpect(jsonPath("$[?(@.email == 'rec.notonboarded@example.com')]").isEmpty())
				.andExpect(jsonPath("$[?(@.email == 'rec.stranger@example.com')]").exists());
	}

}
