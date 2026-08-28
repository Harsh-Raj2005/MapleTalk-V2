package com.mapletalk.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapletalk.dto.FriendRequestResponse;
import com.mapletalk.dto.UserResponse;
import com.mapletalk.entity.FriendRequest;
import com.mapletalk.entity.FriendRequestStatus;
import com.mapletalk.entity.Friendship;
import com.mapletalk.entity.User;
import com.mapletalk.exception.AlreadyFriendsException;
import com.mapletalk.exception.DuplicateFriendRequestException;
import com.mapletalk.exception.FriendRequestAlreadyProcessedException;
import com.mapletalk.exception.FriendRequestNotFoundException;
import com.mapletalk.exception.NotRequestRecipientException;
import com.mapletalk.exception.UserNotFoundException;
import com.mapletalk.repository.FriendRequestRepository;
import com.mapletalk.repository.FriendshipRepository;
import com.mapletalk.repository.UserRepository;

/**
 * Friend-request and friendship business logic. Identity always comes from
 * the authenticated principal's email (passed in by the controller from the
 * SecurityContext) — a path parameter may name the target user or request,
 * but never the acting user.
 */
@Service
public class FriendService {

	private static final Logger log = LoggerFactory.getLogger(FriendService.class);

	private final UserRepository userRepository;
	private final FriendRequestRepository friendRequestRepository;
	private final FriendshipRepository friendshipRepository;

	public FriendService(
			UserRepository userRepository,
			FriendRequestRepository friendRequestRepository,
			FriendshipRepository friendshipRepository) {
		this.userRepository = userRepository;
		this.friendRequestRepository = friendRequestRepository;
		this.friendshipRepository = friendshipRepository;
	}

	@Transactional
	public FriendRequestResponse sendRequest(String email, Long targetUserId) {
		User currentUser = loadByEmail(email);
		User target = userRepository.findById(targetUserId)
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		if (currentUser.getId().equals(target.getId())) {
			throw new IllegalArgumentException("You cannot send a friend request to yourself");
		}
		if (friendshipRepository.existsBetweenUsers(currentUser.getId(), target.getId())) {
			throw new AlreadyFriendsException("You are already friends with this user");
		}
		if (friendRequestRepository
				.findBySenderIdAndRecipientIdAndStatus(currentUser.getId(), target.getId(), FriendRequestStatus.PENDING)
				.isPresent()) {
			throw new DuplicateFriendRequestException("You already have a pending request to this user");
		}
		if (friendRequestRepository
				.findBySenderIdAndRecipientIdAndStatus(target.getId(), currentUser.getId(), FriendRequestStatus.PENDING)
				.isPresent()) {
			throw new DuplicateFriendRequestException("This user has already sent you a friend request");
		}

		FriendRequest saved = friendRequestRepository.saveAndFlush(new FriendRequest(currentUser, target));
		return FriendRequestResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<FriendRequestResponse> getIncomingRequests(String email) {
		User currentUser = loadByEmail(email);
		return friendRequestRepository.findByRecipientIdAndStatus(currentUser.getId(), FriendRequestStatus.PENDING)
				.stream()
				.map(FriendRequestResponse::from)
				.collect(Collectors.toList());
	}

	@Transactional
	public FriendRequestResponse acceptRequest(String email, Long requestId) {
		User currentUser = loadByEmail(email);
		FriendRequest request = loadPendingRequestAsRecipient(currentUser, requestId);

		request.setStatus(FriendRequestStatus.ACCEPTED);
		friendRequestRepository.saveAndFlush(request);

		try {
			friendshipRepository.saveAndFlush(Friendship.between(request.getSender(), request.getRecipient()));
		} catch (DataIntegrityViolationException alreadyFriends) {
			// The unique/canonical-pair constraint caught a race where the
			// friendship already exists — the desired end state (they are
			// friends) already holds, so this is not a failure.
			log.debug("Friendship already existed when accepting request [requestId={}]", requestId);
		}

		return FriendRequestResponse.from(request);
	}

	@Transactional
	public FriendRequestResponse rejectRequest(String email, Long requestId) {
		User currentUser = loadByEmail(email);
		FriendRequest request = loadPendingRequestAsRecipient(currentUser, requestId);

		request.setStatus(FriendRequestStatus.REJECTED);
		friendRequestRepository.saveAndFlush(request);

		return FriendRequestResponse.from(request);
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getFriends(String email) {
		User currentUser = loadByEmail(email);
		return friendshipRepository.findAllInvolvingUser(currentUser.getId()).stream()
				.map(friendship -> UserResponse.from(friendship.other(currentUser)))
				.collect(Collectors.toList());
	}

	private FriendRequest loadPendingRequestAsRecipient(User currentUser, Long requestId) {
		FriendRequest request = friendRequestRepository.findById(requestId)
				.orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found"));

		if (!request.getRecipient().getId().equals(currentUser.getId())) {
			log.warn("Rejected unauthorized friend-request action [requestId={}, actingUserId={}]",
					requestId, currentUser.getId());
			throw new NotRequestRecipientException("You are not authorized to act on this friend request");
		}
		if (request.getStatus() != FriendRequestStatus.PENDING) {
			throw new FriendRequestAlreadyProcessedException("This friend request has already been processed");
		}
		return request;
	}

	private User loadByEmail(String email) {
		// The JWT filter already confirmed this email exists before granting
		// access, so a missing user here means it was deleted mid-session —
		// a genuinely exceptional state, not a normal error path.
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists: " + email));
	}

}
