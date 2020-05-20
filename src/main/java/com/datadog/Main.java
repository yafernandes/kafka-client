package com.datadog;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datadog.trace.api.interceptor.MutableSpan;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

public class Main {

	static final String TRANSACTION_START = "transaction.start";
	static final String TRANSACTION_QUEUE_TIME = "transaction.queue_time";
	static final String TRANSACTION_ELAPSED = "transaction.elapsed";
	private static final Logger logger = LoggerFactory.getLogger("com.datadog.demo");
	static Properties consumerProperties;
	static Properties producerProperties;

	public static void main(String[] args) throws Exception {

		Thread.currentThread().setName("Kafka client");

		String bootstrap = System.getenv("BOOTSTRAP");
		String inTopic = System.getenv("TOPIC_IN");
		String outTopic = System.getenv("TOPIC_OUT");
		String groupId = System.getenv("DD_SERVICE");
		Long producerDelay = Long.parseLong(System.getenv().getOrDefault("PRODUCER_DELAY", "3000"));

		consumerProperties = new Properties();
		consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
		consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		consumerProperties.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "1000");
		consumerProperties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "true");
		consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
		consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		producerProperties = new Properties();
		producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
		producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, groupId);
		producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		if (inTopic != null && outTopic != null) {
			// MIDDLEMAN - PRODUCER and CONSUMER
			try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerProperties);
					KafkaProducer<String, String> producer = new KafkaProducer<String, String>(producerProperties)) {
				consumer.subscribe(Arrays.asList(inTopic));
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofDays(1));
					for (ConsumerRecord<String, String> inRecord : records) {
						ProducerRecord<String, String> outRecord = new ProducerRecord<String, String>(outTopic,
								inRecord.value());
						setQueueTime(inRecord);
						logger.info("Passing message with content [" + inRecord.value() + "]");
						Thread.sleep(40);
						producer.send(outRecord);
						setElapsedTime();
					}
				}
			}
		} else if (inTopic != null) {
			// CONSUMER
			try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerProperties)) {
				consumer.subscribe(Arrays.asList(inTopic));
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofDays(1));
					for (ConsumerRecord<String, String> inRecord : records) {
						inRecord.timestamp();
						setQueueTime(inRecord);
						logger.info("Received message with content [" + inRecord.value() + "]");
						Thread.sleep(40);
						setElapsedTime();
					}
				}
			}

		} else if (outTopic != null) {
			// PRODUCER
			try (KafkaProducer<String, String> producer = new KafkaProducer<String, String>(producerProperties)) {
				while (true) {
					String payload = "The quick brown fox jumps over the lazy dog";
					ProducerRecord<String, String> outRecord = new ProducerRecord<String, String>(outTopic, payload);
					logger.info("Sending message with content [" + payload + "]");
					producer.send(outRecord);
					if (producerDelay != 0) {
						Thread.sleep(producerDelay);
					}
				}
			}
		} else {
			Server server = new Server(8080);
			ServletHandler handler = new ServletHandler();
			server.setHandler(handler);
			handler.addServletWithMapping(ProducerServlet.class, "/send/*");
			server.start();
			server.join();
		}
	}

	static void setStartTime() {
		Span span = GlobalTracer.get().activeSpan();
		if (span instanceof MutableSpan) {
			long startTime = TimeUnit.NANOSECONDS.toMillis(((MutableSpan) span).getStartTime());
			GlobalTracer.get().activeSpan().setBaggageItem(TRANSACTION_START,
					Long.toString(startTime));
		} else {
			logger.warn("SPAN class is " + span.getClass().getCanonicalName());
		}
	}

	private static void setQueueTime(ConsumerRecord<String, String> record) {
		logger.info("Using record timestamp of type " + record.timestampType().name);
		GlobalTracer.get().activeSpan().setTag(TRANSACTION_QUEUE_TIME,
				Long.toString(System.currentTimeMillis() - record.timestamp()));
	}

	private static void setElapsedTime() {
		Span span = GlobalTracer.get().activeSpan();
		String start = span.getBaggageItem(TRANSACTION_START);
		if (start != null) {
			span.setTag(TRANSACTION_ELAPSED, System.currentTimeMillis() - Long.parseLong(start));
		}
	}
}