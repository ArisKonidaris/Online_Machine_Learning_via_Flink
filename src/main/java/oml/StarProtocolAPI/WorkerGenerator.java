package oml.StarProtocolAPI;

import oml.POJOs.Request;

import java.io.Serializable;

public interface WorkerGenerator extends Serializable {
    Object generate(Request request);
}
