package oml.StarTopologyAPI;

import oml.FlinkBackend.POJOs.Request;

import java.io.Serializable;

public interface WorkerGenerator extends Serializable {
    Object generate(Request request);
}
