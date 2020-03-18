package oml.StarTopologyAPI;

import oml.FlinkAPI.POJOs.Request;

import java.io.Serializable;

public interface NodeGenerator extends Serializable {
    Object generate(Request request);
}
