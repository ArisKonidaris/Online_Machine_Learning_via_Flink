package oml.POJOs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public class Prediction implements Serializable {

    Integer mlPipeline;

    Long dataInstanceId;

    Double prediction;

    public Prediction() {
    }

    public Prediction(Integer mlPipeline, Long dataInstanceId, Double prediction) {
        this.mlPipeline = mlPipeline;
        this.dataInstanceId = dataInstanceId;
        this.prediction = prediction;
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JsonProcessingException e) {
            return "Non printable prediction.";
        }
    }

    public String toJsonString() throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public Integer getMlPipeline() {
        return mlPipeline;
    }

    public void setMlPipeline(Integer mlPipeline) {
        this.mlPipeline = mlPipeline;
    }

    public Double getPrediction() {
        return prediction;
    }

    public void setPrediction(Double prediction) {
        this.prediction = prediction;
    }

    public Long getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(Long dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }

}
