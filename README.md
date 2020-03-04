# Online_Machine_Learning_via_Flink

Distributed High Scale Online Machine Learning via Apache Flink

## Getting started

```
$ mvn package
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 36 --replication-factor 4 --topic data
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 36 --replication-factor 4 --topic psMessages
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 1 --replication-factor 4 --topic psMessagesStr
$ kafka/bin/kafka-topics.sh --create --bootstrap-server ip:port --partitions 1 --replication-factor 4 --topic requests
$ flink/bin/flink run /path/to/Online_Machine_Learning_via_Flink/target/oml1.2-0.0.1-SNAPSHOT.jar \
--parallelism 36 \
--dataAddr <data BrokerList> \
--psMessagesAddr <psMessages BrokerList> \
--psMessagesStrAddr <psMessagesStr BrokerList> \
--requestsAddr <requests BrokerList>
```
