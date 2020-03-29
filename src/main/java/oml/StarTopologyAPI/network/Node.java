package oml.StarTopologyAPI.network;

import oml.StarTopologyAPI.sites.NodeId;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;

import java.io.Serializable;

public interface Node extends Mergeable, Serializable {

    void init();

    void receiveQuery(Serializable query);

    void receiveMsg(NodeId source, RemoteCallIdentifier rpc, Serializable message);

    void receiveTuple(Serializable tuple);

}
