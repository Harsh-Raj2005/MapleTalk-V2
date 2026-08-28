package com.mapletalk.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Proves the producer really publishes a correctly-serialized message that a
 * consumer can read back matching — using an in-process embedded broker, not
 * real Aiven. This is deliberately independent of any live credentials so it
 * runs deterministically as part of the normal test suite; the one thing it
 * cannot verify is real Aiven SASL_SSL authentication itself, which requires
 * an actual connection (see the manual live-verification step).
 */
@SpringBootTest(properties = {
		"kafka.enabled=true",
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.security.protocol=PLAINTEXT",
		"spring.kafka.consumer.group-id=friendship-event-test-consumer",
		"spring.kafka.consumer.auto-offset-reset=earliest",
		"spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
		"spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
		"spring.kafka.consumer.properties.spring.json.trusted.packages=com.mapletalk.kafka",
		"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
		"spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@EmbeddedKafka(partitions = 1, topics = { FriendshipEventProducer.TOPIC })
class FriendshipEventKafkaIntegrationTest {

	@Autowired
	private FriendshipEventProducer producer;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	private Consumer<String, FriendshipEvent> testConsumer;

	@AfterEach
	void closeConsumer() {
		if (testConsumer != null) {
			testConsumer.close();
		}
	}

	@Test
	void publishedEventCanBeReadBackWithMatchingContent() {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("friendship-event-verify", "true", embeddedKafkaBroker);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.mapletalk.kafka");
		consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, FriendshipEvent.class.getName());

		testConsumer = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, FriendshipEvent>(
				consumerProps,
				new org.apache.kafka.common.serialization.StringDeserializer(),
				new JsonDeserializer<>(FriendshipEvent.class, false))
				.createConsumer();
		embeddedKafkaBroker.consumeFromAnEmbeddedTopic(testConsumer, FriendshipEventProducer.TOPIC);

		FriendshipEvent published = new FriendshipEvent(42L, 1L, 2L, Instant.now());
		producer.publish(published);

		ConsumerRecord<String, FriendshipEvent> record =
				KafkaTestUtils.getSingleRecord(testConsumer, FriendshipEventProducer.TOPIC);

		assertThat(record.key()).isEqualTo("42");
		assertThat(record.value().friendshipId()).isEqualTo(published.friendshipId());
		assertThat(record.value().userAId()).isEqualTo(published.userAId());
		assertThat(record.value().userBId()).isEqualTo(published.userBId());
	}

}
