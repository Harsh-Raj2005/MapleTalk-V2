package com.mapletalk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mapletalk.entity.FriendRequest;
import com.mapletalk.entity.FriendRequestStatus;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

	List<FriendRequest> findBySenderId(Long senderId);

	List<FriendRequest> findByRecipientId(Long recipientId);

	List<FriendRequest> findByRecipientIdAndStatus(Long recipientId, FriendRequestStatus status);

	// Used to detect an existing request in a specific direction/status —
	// callers check both (me, them) and (them, me) to cover either direction.
	Optional<FriendRequest> findBySenderIdAndRecipientIdAndStatus(
			Long senderId, Long recipientId, FriendRequestStatus status);

}
