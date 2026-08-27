package com.mapletalk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mapletalk.entity.Friendship;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	Optional<Friendship> findByUserAIdAndUserBId(Long userAId, Long userBId);

	List<Friendship> findByUserAIdOrUserBId(Long userAId, Long userBId);

	// Order-independent existence check: callers do not need to know which of
	// the two ids is the canonical "smaller" one.
	@Query("select case when count(f) > 0 then true else false end from Friendship f "
			+ "where (f.userA.id = :firstId and f.userB.id = :secondId) "
			+ "or (f.userA.id = :secondId and f.userB.id = :firstId)")
	boolean existsBetweenUsers(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

	// All friendship rows involving a single given user, regardless of which
	// side of the canonical pair they're on. findByUserAIdOrUserBId doesn't
	// fit this: it takes two independent ids, not "either column = this id".
	@Query("select f from Friendship f where f.userA.id = :userId or f.userB.id = :userId")
	List<Friendship> findAllInvolvingUser(@Param("userId") Long userId);

}
