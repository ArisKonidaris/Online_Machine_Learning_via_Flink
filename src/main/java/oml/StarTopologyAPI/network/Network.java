package oml.StarTopologyAPI.network;

import oml.StarTopologyAPI.sites.NodeId;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.sites.NetworkDescriptor;

import java.io.Serializable;

public interface Network extends Serializable {

    void send(NodeId source, NodeId destination, RemoteCallIdentifier rpc, Serializable message);

    void broadcast(NodeId source, RemoteCallIdentifier rpc, Serializable message);

    NetworkDescriptor describe();

}
