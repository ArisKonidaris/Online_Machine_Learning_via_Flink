package oml.FlinkAPI.POJOs;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * A serializable POJO class representing a request to the Online Machine Learning component
 */
public class Request {

    public Integer id; // The unique flink_worker_id used to identify an ML Pipeline
    public String request; // The request type

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<Preprocessor> preprocessors; // A list of preprocessors. This could be empty

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Learner learner; // A single learner for the ML Pipeline. This should not be empty

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long requestId; // The unique flink_worker_id associated with this request

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> training_configuration; // A helper map

    @JsonIgnore
    public KafkaMetadata metadata;

    public Request() {
    }

    public Request(Integer id, String request, List<Preprocessor> preprocessors, Learner learner) {
        this.id = id;
        this.request = request;
        this.preprocessors = preprocessors;
        this.learner = learner;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public List<Preprocessor> getPreprocessors() {
        return preprocessors;
    }

    public void setPreprocessors(List<Preprocessor> preprocessors) {
        this.preprocessors = preprocessors;
    }

    public Learner getLearner() {
        return learner;
    }

    public void setLearner(Learner learner) {
        this.learner = learner;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Map<String, Object> getTraining_configuration() {
        return training_configuration;
    }

    public void setTraining_configuration(Map<String, Object> training_configuration) {
        this.training_configuration = training_configuration;
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JsonProcessingException e) {
            return "Non printable request.";
        }
    }

    public String toJsonString() throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    @JsonIgnore
    public boolean isValid() {
        if (id == null || id < 0) return false;
        if (request == null ||
                (!request.equals("Create") &&
                        !request.equals("Update") &&
                        !request.equals("Delete") &&
                        !request.equals("Query"))
        ) return false;
        if (request.equals("Create") && learner == null) return false;
        if (request.equals("Update") && preprocessors == null && learner == null) return false;
        if (learner != null && learner.name == null) return false;
        if (preprocessors != null) for (Preprocessor p : preprocessors) if (p.name == null) return false;
        if (training_configuration != null &&
                training_configuration.containsKey("protocol") &&
                !training_configuration.get("protocol").equals("Asynchronous") &&
                !training_configuration.get("protocol").equals("Asynchronous") &&
                !training_configuration.get("protocol").equals("Asynchronous")) return false;
        try {
            if (training_configuration != null &&
                    training_configuration.containsKey("mini_batch_size") &&
                    ((int) training_configuration.get("mini_batch_size")) <= 0 ) return false;
        } catch (Exception e) {
            return false;
        }
        if (request.equals("Query") && requestId == null) return false;
        return true;
    }

    @JsonIgnore
    public void setMetadata(String topic, Integer partition, Long key, Long offset, Long timestamp) {
        metadata = new KafkaMetadata(topic, partition, key, offset, timestamp);
    }

    @JsonIgnore
    public KafkaMetadata getMetadata() {
        return metadata;
    }

    @JsonIgnore
    public void setMetadata(KafkaMetadata metadata) {
        this.metadata = metadata;
    }
}
