package oml.FlinkBipartiteAPI.POJOs;

import java.io.Serializable;

public interface KafkaRecord extends Serializable {

    void setMetadata(String topic, Integer partition, Long key, Long offset, Long timestamp);

}
