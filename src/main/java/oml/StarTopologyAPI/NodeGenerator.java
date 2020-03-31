package oml.StarTopologyAPI;

import oml.FlinkBipartiteAPI.POJOs.Request;

import java.io.Serializable;

public interface NodeGenerator extends Serializable {
    NodeInstance generateSpokeNode(Request request);
    NodeInstance generateHubNode(Request request);
}
