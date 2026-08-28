package com.mapletalk.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a {@link FriendshipEvent} whenever two users become friends.
 * This is a side effect on top of the existing synchronous accept-request
 * flow, not a replacement for it — {@code FriendService.acceptRequest}
 * already persists the friendship and returns a response before this ever
 * runs; publishing failure must never affect that.
 *
 * Disabled entirely when kafka.enabled=false (the test profile), so the
 * existing test suite never attempts a real connection to Aiven.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class FriendshipEventProducer {

	public static final String TOPIC = "friendship-events";

	private static final Logger log = LoggerFactory.getLogger(FriendshipEventProducer.class);

	private final KafkaTemplate<String, FriendshipEvent> kafkaTemplate;

	public FriendshipEventProducer(KafkaTemplate<String, FriendshipEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publish(FriendshipEvent event) {
		try {
			kafkaTemplate.send(TOPIC, String.valueOf(event.friendshipId()), event)
					.whenComplete((result, ex) -> {
						if (ex != null) {
							// Never the friendship id/user ids' business — just that
							// delivery failed. No credentials are ever in this path.
							log.warn("Could not publish friendship event [friendshipId={}]: {}",
									event.friendshipId(), ex.getMessage());
						}
					});
		} catch (Exception ex) {
			// A synchronous failure here (e.g. the producer couldn't even be
			// constructed) must not break the friend-accept API response.
			log.warn("Could not publish friendship event [friendshipId={}]: {}",
					event.friendshipId(), ex.getMessage());
		}
	}

}
