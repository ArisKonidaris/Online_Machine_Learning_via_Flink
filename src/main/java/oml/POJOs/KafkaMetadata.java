package oml.POJOs;

import java.io.Serializable;

public class KafkaMetadata implements Serializable {

    String topic;
    Integer partition;
    Long key;
    Long offset;
    Long timestamp;

    public KafkaMetadata() {
    }

    public KafkaMetadata(String topic, Integer partition, Long key, Long offset, Long timestamp) {
        this.topic = topic;
        this.partition = partition;
        this.key = key;
        this.offset = offset;
        this.timestamp = timestamp;
    }


    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Long getKey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
