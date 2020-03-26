package oml.StarTopologyAPI;

import oml.POJOs.Request;

import java.io.Serializable;

public interface WorkerGenerator extends Serializable {
    Object generateTrainingWorker(Request request);
    Object generatePredictionWorker(Request request);
}
