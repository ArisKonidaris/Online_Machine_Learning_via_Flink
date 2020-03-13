package oml.StarTopologyAPI.network;

import oml.StarTopologyAPI.operations.RemoteCallIdentifier;

import java.io.Serializable;

public interface Node extends Serializable {

    void receiveMsg(Integer source, RemoteCallIdentifier rpc, Serializable message);

    void receiveTuple(Serializable tuple);

    void merge(Node node);

}
