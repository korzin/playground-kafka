package org.korzin.kafka;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@Singleton
public class ProducerFactory {

  @Configuration("kafka.client-id")
  private String kafkaClientId;

  public Producer<Long, String> createProducer(String kafkaBrokers) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaBrokers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    return new KafkaProducer<>(props);
  }
}
