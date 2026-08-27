package com.mapletalk.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.mapletalk.entity.Friendship;
import com.mapletalk.entity.User;

// H2 (MySQL-compatibility mode) integration test — not run against real MySQL.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FriendshipRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FriendshipRepository friendshipRepository;

	@Test
	void persistsFriendshipReferencingBothUsers() {
		User a = userRepository.saveAndFlush(new User("Alice", "alice@example.com", "hash"));
		User b = userRepository.saveAndFlush(new User("Bob", "bob@example.com", "hash"));

		Friendship friendship = friendshipRepository.saveAndFlush(Friendship.between(a, b));

		assertThat(friendship.getId()).isNotNull();
		assertThat(friendshipRepository.existsBetweenUsers(a.getId(), b.getId())).isTrue();
		assertThat(friendshipRepository.existsBetweenUsers(b.getId(), a.getId())).isTrue();
	}

	@Test
	void canonicalOrderingIsIndependentOfArgumentOrder() {
		User a = userRepository.saveAndFlush(new User("Alice", "alice2@example.com", "hash"));
		User b = userRepository.saveAndFlush(new User("Bob", "bob2@example.com", "hash"));

		Friendship viaAB = Friendship.between(a, b);
		Friendship viaBA = Friendship.between(b, a);

		assertThat(viaAB.getUserA().getId()).isEqualTo(viaBA.getUserA().getId());
		assertThat(viaAB.getUserB().getId()).isEqualTo(viaBA.getUserB().getId());
	}

	@Test
	void rejectsSelfFriendshipAtApplicationLevel() {
		User a = userRepository.saveAndFlush(new User("Alice", "alice3@example.com", "hash"));

		assertThatThrownBy(() -> Friendship.between(a, a))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsMirroredDuplicateFriendshipPair() {
		User a = userRepository.saveAndFlush(new User("Alice", "alice4@example.com", "hash"));
		User b = userRepository.saveAndFlush(new User("Bob", "bob4@example.com", "hash"));

		friendshipRepository.saveAndFlush(Friendship.between(a, b));

		// Same pair, arguments reversed — must collide with the row above
		// because Friendship.between() always canonicalizes ordering.
		assertThatThrownBy(() -> friendshipRepository.saveAndFlush(Friendship.between(b, a)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
