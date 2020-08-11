package org.korzin.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ETLRunner {

  @Configuration("etl.message.count")
  private String messageCount; // TODO inject as integer

  @Configuration("kafka.max-no-message-found-count")
  private String maxNoMsgFoundTries;

  @Configuration("kafka.topic-name")
  private String topicName;

  @Inject private ZookeeperClient zookeeperClient;
  @Inject private ProducerFactory producerFactory;

  public void doWork() {
    String brokersEndpoints = "172.17.0.1:32771"; // getBrokerEndpointsField();
    try {
      //      runProducer(brokersEndpoints);
      runConsumer(brokersEndpoints);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private String getBrokerEndpointsField() {
    List<JsonNode> brokers;
    try {
      brokers = zookeeperClient.fetchBrokersInfo();
    } catch (IOException | KeeperException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    System.out.println(">>> brokers: " + brokers);
    String brokersEndpoints =
        brokers.stream()
            .map(broker -> broker.get("host").asText() + ":" + broker.get("port").asText())
            .collect(Collectors.joining(","));
    System.out.println(">>> brokers(single line): " + brokersEndpoints);
    return brokersEndpoints;
  }

  //  private void runConsumer() {
  //    Consumer<Long, String> consumer = ConsumerFactory.createConsumer();
  //
  //    int noMessageToFetch = 0;
  //
  //    while (true) {
  //      final ConsumerRecords<Long, String> consumerRecords =
  // consumer.poll(Duration.ofMillis(1000));
  //      if (consumerRecords.count() == 0) {
  //        noMessageToFetch++;
  //        if (noMessageToFetch > KafkaConnConfig.MAX_NO_MESSAGE_FOUND_COUNT) break;
  //        else continue;
  //      }

  private void runConsumer(String kafkaBrokers) {
    Consumer<Long, String> consumer =
        App.injector.get().getInstance(ConsumerFactory.class).createConsumer(kafkaBrokers);
    int noMessageToFetch = 0;

    while (true) {
      final ConsumerRecords<Long, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
      if (consumerRecords.count() == 0) {
        noMessageToFetch++;
        if (noMessageToFetch > Integer.parseInt(maxNoMsgFoundTries)) {
          break;
        } else {
          continue;
        }
      }

      consumerRecords.forEach(
          record -> {
            System.out.println("Record Key " + record.key());
            System.out.println("Record value " + record.value());
            System.out.println("Record partition " + record.partition());
            System.out.println("Record offset " + record.offset());
          });
      consumer.commitAsync();
    }
    consumer.close();
  }

  private void runProducer(String kafkaBrokers) {
    Producer<Long, String> producer = producerFactory.createProducer(kafkaBrokers);
    for (int i = 0; i < Integer.parseInt(messageCount); i++) {
      final ProducerRecord<Long, String> record =
          new ProducerRecord<>(topicName, "This is record " + i);
      try {
        RecordMetadata metadata = producer.send(record).get();
        System.out.println(
            "Record sent with key "
                + i
                + " to partition "
                + metadata.partition()
                + " with offset "
                + metadata.offset());
      } catch (ExecutionException | InterruptedException e) {
        System.out.println("Error in sending record");
        System.out.println(e);
      }
    }
  }
}
