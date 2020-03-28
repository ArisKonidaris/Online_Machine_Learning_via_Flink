package oml.FlinkBipartiteAPI.POJOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * A serializable POJO class representing a response
 * from the Online Machine Leaning component.
 */
public class QueryResponse {

    /**
     * The unique id associated with this request. This is the answer to a "Query" request
     * to the Online Machine Learning component. This responseId should be equal to the
     * corresponding requestId that it answers.
     */
    public long responseId;

    public int id; // The unique id of the ML Pipeline that provided this answer.

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<Preprocessor> preprocessors; // A list of preprocessors. This could be empty.

    public Learner learner; // A single learner for the ML Pipeline. This should not be empty.

    public String protocol; // The distributed algorithm used to train a ML Pipeline in parallel.

    public Long dataFitted; // The number of fitted data points.

    public Double score; // The query of the Machine Leaning algorithm.

    public QueryResponse() {
    }

    public QueryResponse(long responseId,
                         int id,
                         List<Preprocessor> preprocessors,
                         Learner learner,
                         String protocol,
                         Long dataFitted,
                         Double score) {
        this.responseId = responseId;
        this.id = id;
        this.preprocessors = preprocessors;
        this.learner = learner;
        this.protocol = protocol;
        this.dataFitted = dataFitted;
        this.score = score;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public long getResponseId() {
        return responseId;
    }

    public void setResponseId(long responseId) {
        this.responseId = responseId;
    }

    public Long getDataFitted() {
        return dataFitted;
    }

    public void setDataFitted(Long dataFitted) {
        this.dataFitted = dataFitted;
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JsonProcessingException e) {
            return "Non printable response.";
        }
    }

    public String toJsonString() throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

}
