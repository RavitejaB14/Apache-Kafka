import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer

import java.time.Duration
import java.util.{Collections, Properties}
import scala.collection.JavaConverters._

object KafkaConsumer {
  def main(args: Array[String]): Unit = {

    // Kafka Consumer Properties
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "scala-consumer-groups")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")


    // Create the kafka Consumer
    val consumer = new KafkaConsumer[String, String](props)

    // subscribe to a topic
    val  topic = "myTopic"
    consumer.subscribe(Collections.singletonList(topic))

    // polling
    try{
      while(true){
        // poll new records
        val records = consumer.poll(Duration.ofMillis(1000))

        for (record <- records.asScala){
          println(s"Consumer Record: Key=${record.key()}, Value=${record.value()}, Partition=${record.partition()}, Offset=${record.offset()}")
        }
      }
    }catch{
      case e: Exception => e.printStackTrace()
    }finally {
      consumer.close()
    }
  }
}
