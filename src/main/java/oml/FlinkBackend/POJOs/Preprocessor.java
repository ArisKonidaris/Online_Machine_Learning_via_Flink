package oml.FlinkBackend.POJOs;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * A serializable POJO class representing a preprocessor (i.e. Polynomial Features)
 */
public class Preprocessor extends Transformer {

    public Preprocessor() {
        super();
    }

    public Preprocessor(String name, Map<String, Object> hyperparameters, Map<String, Object> parameters) {
        super(name, hyperparameters, parameters);
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JsonProcessingException e) {
            return "Non printable preprocessor.";
        }
    }

    public String toJsonString() throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }
}