package com.mapletalk.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapletalk.dto.OnboardingRequest;
import com.mapletalk.dto.UserResponse;
import com.mapletalk.entity.Friendship;
import com.mapletalk.entity.User;
import com.mapletalk.repository.FriendshipRepository;
import com.mapletalk.repository.UserRepository;

/**
 * Business logic for the "current user" — profile lookup, onboarding, and
 * friend recommendations. Never accepts a client-supplied user id; identity
 * always comes from the authenticated principal's email, passed in by the
 * controller from the SecurityContext.
 */
@Service
public class UserService {

	private final UserRepository userRepository;
	private final FriendshipRepository friendshipRepository;

	public UserService(UserRepository userRepository, FriendshipRepository friendshipRepository) {
		this.userRepository = userRepository;
		this.friendshipRepository = friendshipRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(String email) {
		return UserResponse.from(loadByEmail(email));
	}

	@Transactional
	public UserResponse updateOnboarding(String email, OnboardingRequest request) {
		User user = loadByEmail(email);

		String fullName = blankToNull(request.fullName());
		String bio = blankToNull(request.bio());
		String nativeLanguage = blankToNull(request.nativeLanguage());
		String learningLanguage = blankToNull(request.learningLanguage());
		String location = blankToNull(request.location());

		if (fullName == null || bio == null || nativeLanguage == null
				|| learningLanguage == null || location == null) {
			throw new IllegalArgumentException(
					"fullName, bio, nativeLanguage, learningLanguage and location are all required");
		}

		user.setFullName(fullName);
		user.setBio(bio);
		user.setNativeLanguage(nativeLanguage);
		user.setLearningLanguage(learningLanguage);
		user.setLocation(location);
		user.setOnboarded(true);

		// saveAndFlush (not save): @UpdateTimestamp only stamps the new
		// updatedAt value during the actual flush. On an already-managed
		// entity, plain save() defers that flush to transaction commit —
		// which happens after this method has already returned the DTO,
		// so the response would show a stale updatedAt otherwise.
		return UserResponse.from(userRepository.saveAndFlush(user));
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getRecommendedUsers(String email) {
		User currentUser = loadByEmail(email);

		Set<Long> excludedIds = new LinkedHashSet<>();
		excludedIds.add(currentUser.getId());

		for (Friendship friendship : friendshipRepository.findAllInvolvingUser(currentUser.getId())) {
			Long friendId = friendship.getUserA().getId().equals(currentUser.getId())
					? friendship.getUserB().getId()
					: friendship.getUserA().getId();
			excludedIds.add(friendId);
		}

		return userRepository.findByIdNotInAndIsOnboardedTrue(excludedIds).stream()
				.map(UserResponse::from)
				.collect(Collectors.toList());
	}

	private User loadByEmail(String email) {
		// The JWT filter already confirmed this email exists before granting
		// access, so a missing user here means it was deleted mid-session —
		// a genuinely exceptional state, not a normal error path.
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists: " + email));
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

}
