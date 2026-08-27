package com.mapletalk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mapletalk.entity.FriendRequest;
import com.mapletalk.entity.FriendRequestStatus;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

	List<FriendRequest> findBySenderId(Long senderId);

	List<FriendRequest> findByRecipientId(Long recipientId);

	List<FriendRequest> findByRecipientIdAndStatus(Long recipientId, FriendRequestStatus status);

}
