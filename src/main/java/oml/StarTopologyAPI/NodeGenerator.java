package oml.StarTopologyAPI;

import oml.FlinkBipartiteAPI.POJOs.Request;

import java.io.Serializable;

public interface NodeGenerator extends Serializable {
    NodeInstance generate(Request request);
}
