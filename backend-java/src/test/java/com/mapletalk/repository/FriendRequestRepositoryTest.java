package com.mapletalk.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.mapletalk.entity.FriendRequest;
import com.mapletalk.entity.FriendRequestStatus;
import com.mapletalk.entity.User;

// H2 (MySQL-compatibility mode) integration test — not run against real MySQL.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FriendRequestRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FriendRequestRepository friendRequestRepository;

	@Test
	void persistsRequestReferencingSenderAndRecipient() {
		User sender = userRepository.saveAndFlush(new User("Sender", "sender@example.com", "hash"));
		User recipient = userRepository.saveAndFlush(new User("Recipient", "recipient@example.com", "hash"));

		FriendRequest request = friendRequestRepository.saveAndFlush(new FriendRequest(sender, recipient));

		assertThat(request.getId()).isNotNull();
		assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
		assertThat(request.getSender().getId()).isEqualTo(sender.getId());
		assertThat(request.getRecipient().getId()).isEqualTo(recipient.getId());

		assertThat(friendRequestRepository.findBySenderId(sender.getId())).hasSize(1);
		assertThat(friendRequestRepository.findByRecipientIdAndStatus(recipient.getId(), FriendRequestStatus.PENDING))
				.hasSize(1);
	}

}
