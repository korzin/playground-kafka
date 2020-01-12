set -e 

CR_NAME="kafka-docker-container"

docker-compose down 

echo
echo " <><> Launch Kafka cluster (+ zookeeper)";
echo

docker-compose up -d

echo
echo " <><> Cluster has been launched";
echo

#tail -f `docker inspect --format='{{.LogPath}}' kafka_v1`


