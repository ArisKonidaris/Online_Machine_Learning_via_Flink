package oml.FlinkBipartiteAPI.POJOs;

public interface Validatable extends KafkaRecord {

    boolean isValid();

}
