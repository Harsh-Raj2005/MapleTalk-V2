package com.mapletalk.kafka;

import java.time.Instant;

/**
 * Published when two users become friends. Deliberately minimal — just
 * enough for a consumer to know who connected and when, not a full
 * snapshot of either user's profile.
 */
public record FriendshipEvent(Long friendshipId, Long userAId, Long userBId, Instant occurredAt) {
}
