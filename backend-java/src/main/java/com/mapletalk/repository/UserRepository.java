package com.mapletalk.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mapletalk.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	// excludedIds must include the current user's own id — the caller is
	// expected to pass a non-empty set (see UserService.getRecommendedUsers).
	List<User> findByIdNotInAndIsOnboardedTrue(Collection<Long> excludedIds);

}
