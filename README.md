# Online_Machine_Learning_via_Flink

Distributed High Scale Online Machine Learning via Apache Flink

## Getting started

```
$ mvn package
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 36 --replication-factor 4 --topic trainingData
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 36 --replication-factor 4 --topic forecastingData
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 1 --replication-factor 4 --topic requests
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 1 --replication-factor 4 --topic responses
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 36 --replication-factor 4 --topic predictions
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 36 --replication-factor 4 --topic psMessages

$ flink/bin/flink run /path/to/Online_Machine_Learning_via_Flink/target/oml1.2-0.0.1-SNAPSHOT.jar \
--parallelism 36 \
--trainingDataAddr <training data BrokerList> \
--forecastingDataAddr <forecasting data BrokerList> \
--requestsAddr <requests BrokerList>
--responsesAddr <responses BrokerList>
--predictionsAddr <predictions BrokerList>
--psMessagesAddr <psMessages BrokerList> \
```
