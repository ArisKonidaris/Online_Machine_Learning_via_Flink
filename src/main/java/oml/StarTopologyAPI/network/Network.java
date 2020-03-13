package oml.StarTopologyAPI.network;

import oml.StarTopologyAPI.operations.RemoteCallIdentifier;

import java.io.Serializable;

public interface Network extends Serializable {

    void send(Integer source, Integer destination, RemoteCallIdentifier rpc, Serializable message);

    void broadcast(Integer source, RemoteCallIdentifier rpc, Serializable message);

    void describe();

}
