package com.mapletalk.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link FriendshipEvent}s. MapleTalk has no notification or
 * background-job system yet for this to hand off to, so this deliberately
 * does the smallest honest thing: log the event clearly enough to prove the
 * full producer → Aiven Kafka → consumer pipeline actually works. Building
 * a fuller consumer (e.g. a notification write) belongs to whichever future
 * phase introduces that system.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class FriendshipEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(FriendshipEventConsumer.class);

	@KafkaListener(topics = FriendshipEventProducer.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
	public void onFriendshipEvent(FriendshipEvent event) {
		try {
			log.info("Received friendship event [friendshipId={}, userAId={}, userBId={}, occurredAt={}]",
					event.friendshipId(), event.userAId(), event.userBId(), event.occurredAt());
		} catch (Exception ex) {
			// A malformed/unexpected event must not crash the listener
			// container or take down the consumer thread.
			log.error("Failed to process friendship event: {}", ex.getMessage(), ex);
		}
	}

}
