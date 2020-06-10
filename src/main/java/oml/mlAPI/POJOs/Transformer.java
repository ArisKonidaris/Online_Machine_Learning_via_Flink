package oml.mlAPI.POJOs;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * A serializable POJO class representing a transformer (e.i. Preprocessor or Learner).
 */
public class Transformer {
    public String name; // The name of the preprocessor

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> hyperparameters; // The hyper parameters of the preprocessor

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> parameters; // The parameters of the preprocessor

    public Transformer() {
    }

    public Transformer(String name, Map<String, Object> hyperparameters, Map<String, Object> parameters) {
        this.name = name;
        this.hyperparameters = hyperparameters;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getHyperparameters() {
        return hyperparameters;
    }

    public void setHyperparameters(Map<String, Object> hyperparameters) {
        this.hyperparameters = hyperparameters;
    }
}