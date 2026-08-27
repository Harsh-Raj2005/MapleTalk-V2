package com.mapletalk.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "friend_requests", indexes = {
		@Index(name = "idx_friend_requests_sender", columnList = "sender_id"),
		@Index(name = "idx_friend_requests_recipient", columnList = "recipient_id"),
		@Index(name = "idx_friend_requests_status", columnList = "status")
})
public class FriendRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_id", nullable = false)
	private User sender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FriendRequestStatus status = FriendRequestStatus.PENDING;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected FriendRequest() {
		// required by JPA
	}

	public FriendRequest(User sender, User recipient) {
		this.sender = sender;
		this.recipient = recipient;
		this.status = FriendRequestStatus.PENDING;
	}

	public Long getId() {
		return id;
	}

	public User getSender() {
		return sender;
	}

	public User getRecipient() {
		return recipient;
	}

	public FriendRequestStatus getStatus() {
		return status;
	}

	public void setStatus(FriendRequestStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
