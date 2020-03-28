package oml.FlinkBipartiteAPI.POJOs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

/**
 * A serializable POJO class representing a data point for training or predicting.
 */
public class DataInstance implements Serializable {

    /**
     * A unique id for the data point needed only for the prediction of an unlabeled data point.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long id;

    public List<Double> numericFeatures; // The numerical features of the data point.

    public List<String> discreteFeatures; // The discrete features of the data point.

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double target; // The label/target of the data point.

    public String operation; // The operation of this data point.

    @JsonIgnore
    public KafkaMetadata metadata;

    public DataInstance() {
    }

    public DataInstance(List<Double> numericFeatures, Double target) {
        this(null, numericFeatures, null, target, "training");
    }

    public DataInstance(List<Double> numericFeatures, List<String> discreteFeatures, Double target) {
        this(null, numericFeatures, discreteFeatures, target, "training");
    }

    public DataInstance(Long id,
                        List<Double> numericFeatures,
                        List<String> discreteFeatures,
                        Double target,
                        String operation) {
        this.id = id;
        this.numericFeatures = numericFeatures;
        this.discreteFeatures = discreteFeatures;
        this.target = target;
        this.operation = operation;
    }


    public Double getTarget() {
        return target;
    }

    public void setTarget(Double target) {
        this.target = target;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<Double> getNumericFeatures() {
        return numericFeatures;
    }

    public void setNumericFeatures(List<Double> numericFeatures) {
        this.numericFeatures = numericFeatures;
    }

    public List<String> getDiscreteFeatures() {
        return discreteFeatures;
    }

    public void setDiscreteFeatures(List<String> discreteFeatures) {
        this.discreteFeatures = discreteFeatures;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public KafkaMetadata getMetadata() {
        return metadata;
    }

    @JsonIgnore
    public void setMetadata(KafkaMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JsonProcessingException e) {
            return "Non printable data point.";
        }
    }

    public String toJsonString() throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    @JsonIgnore
    public boolean isValid() {
        if (operation == null || (!operation.equals("training") && !operation.equals("forecasting"))) return false;
        if (
                (numericFeatures == null || numericFeatures.size() == 0) &&
                        (discreteFeatures == null || discreteFeatures.size() == 0)
        ) return false;
        if (operation.equals("forecasting") && target != null && id == null) return false;
        return true;
    }

    @JsonIgnore
    public void setMetadata(String topic, Integer partition, Long key, Long offset, Long timestamp) {
        metadata = new KafkaMetadata(topic, partition, key, offset, timestamp);
    }

}
