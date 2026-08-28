package com.mapletalk.entity;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A friendship is stored once per pair, with the lower user id always in
 * {@code userA} and the higher in {@code userB}. This canonical ordering —
 * rather than a plain UNIQUE(user_a_id, user_b_id) — is what actually makes
 * (A, B) and (B, A) collide as the same row instead of two mirrored rows.
 * Use {@link #between(User, User)} rather than a public constructor so the
 * ordering invariant can never be bypassed.
 */
@Entity
@Table(name = "friendships", uniqueConstraints = {
		@UniqueConstraint(name = "uk_friendships_pair", columnNames = { "user_a_id", "user_b_id" })
}, indexes = {
		@Index(name = "idx_friendships_user_a", columnList = "user_a_id"),
		@Index(name = "idx_friendships_user_b", columnList = "user_b_id")
})
@Check(constraints = "user_a_id < user_b_id")
public class Friendship {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_a_id", nullable = false)
	private User userA;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_b_id", nullable = false)
	private User userB;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Friendship() {
		// required by JPA
	}

	private Friendship(User userA, User userB) {
		this.userA = userA;
		this.userB = userB;
	}

	/**
	 * Creates a friendship between two users, ordering them canonically by id
	 * so that {@code between(x, y)} and {@code between(y, x)} always produce
	 * an equivalent row. Rejects self-friendship.
	 */
	public static Friendship between(User first, User second) {
		Objects.requireNonNull(first, "first user must not be null");
		Objects.requireNonNull(second, "second user must not be null");
		Objects.requireNonNull(first.getId(), "first user must already be persisted");
		Objects.requireNonNull(second.getId(), "second user must already be persisted");

		if (first.getId().equals(second.getId())) {
			throw new IllegalArgumentException("A user cannot be friends with themselves");
		}

		return first.getId() < second.getId()
				? new Friendship(first, second)
				: new Friendship(second, first);
	}

	public Long getId() {
		return id;
	}

	public User getUserA() {
		return userA;
	}

	public User getUserB() {
		return userB;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	/** Given one side of the friendship, returns the other user. */
	public User other(User user) {
		return userA.getId().equals(user.getId()) ? userB : userA;
	}

}
