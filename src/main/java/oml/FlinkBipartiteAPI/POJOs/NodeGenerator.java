package oml.FlinkBipartiteAPI.POJOs;

import oml.StarTopologyAPI.NodeInstance;

import java.io.Serializable;

public interface NodeGenerator extends Serializable {
    NodeInstance generateSpokeNode(Request request);
    NodeInstance generateHubNode(Request request);
}
