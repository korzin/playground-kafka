package org.korzin.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.inject.Singleton;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ZookeeperClient {

  private static final ObjectReader mapper = new ObjectMapper().reader();

  public static void main(String[] args) throws Exception {
    ZooKeeper zk = new ZooKeeper("localhost:2181", 10000, null);
    List<String> ids = zk.getChildren("/brokers/ids", false);
    for (String id : ids) {
      JsonNode brokerInfo =
          mapper.readTree(new String(zk.getData("/brokers/ids/" + id, false, null)));
      String brokerAddress =
          brokerInfo.get("host").asText() + ":" + brokerInfo.get("port").asText();
      System.out.println(id + ", brokerAddress: " + brokerAddress);
    }
  }

  public List<JsonNode> fetchBrokersInfo()
      throws IOException, KeeperException, InterruptedException {
    ZooKeeper zk = new ZooKeeper("localhost:2181", 10000, null);

    List<String> ids = zk.getChildren("/brokers/ids", false);
    List<JsonNode> brokers = new ArrayList<>();

    for (String id : ids) {
      JsonNode brokerInfo =
          mapper.readTree(new String(zk.getData("/brokers/ids/" + id, false, null)));

      String brokerAddress =
          brokerInfo.get("host").asText() + ":" + brokerInfo.get("port").asText();
      System.out.println(id + ", brokerAddress: " + brokerAddress);
      brokers.add(brokerInfo);
    }

    return brokers;
  }
}
