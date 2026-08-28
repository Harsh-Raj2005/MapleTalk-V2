package com.mapletalk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.mapletalk.dto.FriendRequestResponse;
import com.mapletalk.dto.UserResponse;
import com.mapletalk.entity.FriendRequestStatus;
import com.mapletalk.entity.User;
import com.mapletalk.exception.AlreadyFriendsException;
import com.mapletalk.exception.DuplicateFriendRequestException;
import com.mapletalk.exception.FriendRequestAlreadyProcessedException;
import com.mapletalk.exception.NotRequestRecipientException;
import com.mapletalk.exception.UserNotFoundException;
import com.mapletalk.kafka.FriendshipEvent;
import com.mapletalk.kafka.FriendshipEventProducer;
import com.mapletalk.repository.FriendshipRepository;
import com.mapletalk.repository.UserRepository;

// Full Spring context against the H2 test database — real persistence for
// every business rule, not mocks.
@SpringBootTest
class FriendServiceTest {

	@Autowired
	private FriendService friendService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FriendshipRepository friendshipRepository;

	// kafka.enabled=false in the test profile means the real
	// FriendshipEventProducer bean doesn't exist at all; @MockBean adds a
	// mock in its place regardless, so FriendService's
	// Optional<FriendshipEventProducer> resolves to Optional.of(mock).
	@MockBean
	private FriendshipEventProducer friendshipEventProducer;

	private User user(String email) {
		return userRepository.saveAndFlush(new User("Fixture " + email, email, "irrelevant-hash"));
	}

	@Test
	void sendRequestCreatesPendingRequest() {
		User alice = user("fr.alice1@example.com");
		User bob = user("fr.bob1@example.com");

		FriendRequestResponse response = friendService.sendRequest(alice.getEmail(), bob.getId());

		assertThat(response.status()).isEqualTo(FriendRequestStatus.PENDING);
		assertThat(response.sender().email()).isEqualTo(alice.getEmail());
		assertThat(response.recipient().email()).isEqualTo(bob.getEmail());
	}

	@Test
	void sendRequestToSelfIsRejected() {
		User alice = user("fr.alice2@example.com");

		assertThatThrownBy(() -> friendService.sendRequest(alice.getEmail(), alice.getId()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void sendRequestToNonexistentUserIsRejected() {
		User alice = user("fr.alice3@example.com");

		assertThatThrownBy(() -> friendService.sendRequest(alice.getEmail(), 999_999L))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void duplicatePendingRequestSameDirectionIsRejected() {
		User alice = user("fr.alice4@example.com");
		User bob = user("fr.bob4@example.com");
		friendService.sendRequest(alice.getEmail(), bob.getId());

		assertThatThrownBy(() -> friendService.sendRequest(alice.getEmail(), bob.getId()))
				.isInstanceOf(DuplicateFriendRequestException.class);
	}

	@Test
	void duplicatePendingRequestOppositeDirectionIsRejected() {
		User alice = user("fr.alice5@example.com");
		User bob = user("fr.bob5@example.com");
		friendService.sendRequest(alice.getEmail(), bob.getId());

		assertThatThrownBy(() -> friendService.sendRequest(bob.getEmail(), alice.getId()))
				.isInstanceOf(DuplicateFriendRequestException.class);
	}

	@Test
	void requestBetweenExistingFriendsIsRejected() {
		User alice = user("fr.alice6@example.com");
		User bob = user("fr.bob6@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());
		friendService.acceptRequest(bob.getEmail(), request.id());

		assertThatThrownBy(() -> friendService.sendRequest(alice.getEmail(), bob.getId()))
				.isInstanceOf(AlreadyFriendsException.class);
	}

	@Test
	void incomingRequestsOnlyReturnsPendingRequestsAddressedToCurrentUser() {
		User alice = user("fr.alice7@example.com");
		User bob = user("fr.bob7@example.com");
		User carol = user("fr.carol7@example.com");

		friendService.sendRequest(alice.getEmail(), bob.getId()); // -> bob incoming
		friendService.sendRequest(carol.getEmail(), alice.getId()); // unrelated to bob

		List<FriendRequestResponse> bobsIncoming = friendService.getIncomingRequests(bob.getEmail());

		assertThat(bobsIncoming).hasSize(1);
		assertThat(bobsIncoming.get(0).sender().email()).isEqualTo(alice.getEmail());
	}

	@Test
	void acceptByNonRecipientIsRejected() {
		User alice = user("fr.alice8@example.com");
		User bob = user("fr.bob8@example.com");
		User mallory = user("fr.mallory8@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());

		assertThatThrownBy(() -> friendService.acceptRequest(mallory.getEmail(), request.id()))
				.isInstanceOf(NotRequestRecipientException.class);
	}

	@Test
	void acceptByRecipientCreatesFriendshipWithCorrectUsers() {
		User alice = user("fr.alice9@example.com");
		User bob = user("fr.bob9@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());

		FriendRequestResponse accepted = friendService.acceptRequest(bob.getEmail(), request.id());

		assertThat(accepted.status()).isEqualTo(FriendRequestStatus.ACCEPTED);
		assertThat(friendshipRepository.existsBetweenUsers(alice.getId(), bob.getId())).isTrue();
	}

	@Test
	void acceptingRequestPublishesFriendshipEvent() {
		User alice = user("fr.alice-kafka@example.com");
		User bob = user("fr.bob-kafka@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());

		friendService.acceptRequest(bob.getEmail(), request.id());

		verify(friendshipEventProducer, times(1)).publish(any(FriendshipEvent.class));
	}

	@Test
	void acceptingAlreadyProcessedRequestIsRejected() {
		User alice = user("fr.alice10@example.com");
		User bob = user("fr.bob10@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());
		friendService.acceptRequest(bob.getEmail(), request.id());

		assertThatThrownBy(() -> friendService.acceptRequest(bob.getEmail(), request.id()))
				.isInstanceOf(FriendRequestAlreadyProcessedException.class);
	}

	@Test
	void rejectByRecipientDoesNotCreateFriendship() {
		User alice = user("fr.alice11@example.com");
		User bob = user("fr.bob11@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());

		FriendRequestResponse rejected = friendService.rejectRequest(bob.getEmail(), request.id());

		assertThat(rejected.status()).isEqualTo(FriendRequestStatus.REJECTED);
		assertThat(friendshipRepository.existsBetweenUsers(alice.getId(), bob.getId())).isFalse();
	}

	@Test
	void rejectByNonRecipientIsRejected() {
		User alice = user("fr.alice12@example.com");
		User bob = user("fr.bob12@example.com");
		User mallory = user("fr.mallory12@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());

		assertThatThrownBy(() -> friendService.rejectRequest(mallory.getEmail(), request.id()))
				.isInstanceOf(NotRequestRecipientException.class);
	}

	@Test
	void friendsAreVisibleFromBothSidesWithoutDuplicatesOrSelf() {
		User alice = user("fr.alice13@example.com");
		User bob = user("fr.bob13@example.com");
		FriendRequestResponse request = friendService.sendRequest(alice.getEmail(), bob.getId());
		friendService.acceptRequest(bob.getEmail(), request.id());

		List<UserResponse> alicesFriends = friendService.getFriends(alice.getEmail());
		List<UserResponse> bobsFriends = friendService.getFriends(bob.getEmail());

		assertThat(alicesFriends).extracting(UserResponse::email).containsExactly(bob.getEmail());
		assertThat(bobsFriends).extracting(UserResponse::email).containsExactly(alice.getEmail());
	}

}
