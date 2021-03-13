package datadog.ese.kafka.client;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class Producer extends KafkaClient {

    private final String topic;

    public Producer(final String bootstrap, final String topic) {
        setBootstrap(bootstrap);
        this.topic = topic;
    }

    @Override
    public void run() {
        try (KafkaProducer<String, String> producer = new KafkaProducer<String, String>(getProducerProperties())) {
            while (true) {
                final ProducerRecord<String, String> outRecord = new ProducerRecord<String, String>(topic, PAYLOAD);
                logger.info("Sending message with content [" + PAYLOAD + "]");
                sleep();
                producer.send(outRecord);
                sleep(500);
            }
        }
    }
}
