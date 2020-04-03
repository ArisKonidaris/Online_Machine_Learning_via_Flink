package oml.FlinkBipartiteAPI.POJOs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * A serializable POJO class representing a machine learning algorithm (e.i. Passive Aggressive Classifier).
 */
public class Learner extends Transformer {
    public Learner() {
        super();
    }

    public Learner(String name, Map<String, Object> hyperparameters, Map<String, Object> parameters) {
        super(name, hyperparameters, parameters);
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JsonProcessingException e) {
            return "Non printable learner.";
        }
    }

    public String toJsonString() throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }
}