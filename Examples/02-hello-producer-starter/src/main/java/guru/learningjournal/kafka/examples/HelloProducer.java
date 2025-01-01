package guru.learningjournal.kafka.examples;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class HelloProducer {

    private static final Logger logger = LogManager.getLogger();
    public static void main(String[] args) {
        logger.info("Creating Kafka Producer.....");

        Properties props = new Properties();
        props.put(ProducerConfig.CLIENT_ID_CONFIG, AppConfigs.applicationID); // to Track the Source of the Message
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfigs.bootstrapServers); // Establising the Initial connection with kafka Cluster.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // Kafka Message must have a key/Value Structure.

        // Instance of Kafka Producer
        KafkaProducer<Integer,String> producer = new KafkaProducer<Integer, String>(props);

        logger.info("Start Sending Messages....");
        for (int i=0; i< AppConfigs.numEvents; i++){
            producer.send(new ProducerRecord<>(AppConfigs.topicName, i, "SimpleMessage-" + i));
        }

        logger.info("Finished Sending Messages.. Closing the producer...");
        producer.close(); // Producer consists of some buffer space and background I/O Thread. If we donot close the producer, you will leak the resources created by the producer.

    }
}
