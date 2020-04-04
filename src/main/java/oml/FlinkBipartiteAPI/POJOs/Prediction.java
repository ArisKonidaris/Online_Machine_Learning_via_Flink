package oml.FlinkBipartiteAPI.POJOs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public class Prediction implements Serializable {

    Integer mlPipeline;

    DataInstance dataPoint;

    Double prediction;

    public Prediction() {
    }

    public Prediction(Integer mlPipeline, DataInstance dataPoint, Double prediction) {
        this.mlPipeline = mlPipeline;
        this.dataPoint = dataPoint;
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

    public DataInstance getDataPoint() {
        return dataPoint;
    }

    public void setDataInstanceId(DataInstance dataPoint) {
        this.dataPoint = dataPoint;
    }

}
