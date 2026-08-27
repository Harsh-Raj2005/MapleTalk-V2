package com.mapletalk.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.mapletalk.entity.User;

// H2 (MySQL-compatibility mode) integration test — not run against real MySQL.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void persistsAndReloadsUser() {
		User user = new User("Ada Lovelace", "ada@example.com", "irrelevant-for-this-phase");
		user.setNativeLanguage("English");
		user.setLearningLanguage("French");

		User saved = userRepository.saveAndFlush(user);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(userRepository.findByEmail("ada@example.com")).isPresent();
	}

	@Test
	void rejectsDuplicateEmail() {
		userRepository.saveAndFlush(new User("First User", "duplicate@example.com", "hash-1"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("Second User", "duplicate@example.com", "hash-2")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
