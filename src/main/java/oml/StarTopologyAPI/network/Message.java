package oml.StarTopologyAPI.network;

import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.sites.NodeId;

import java.io.Serializable;

public class Message implements Serializable {

    public NodeId source;
    public RemoteCallIdentifier rpc;
    public Serializable message;

    public Message() {
        source = null;
        rpc = null;
        message = null;
    }

    public Message(NodeId source, RemoteCallIdentifier rpc, Serializable message) {
        this.source = source;
        this.rpc = rpc;
        this.message = message;
    }


    public NodeId getSource() {
        return source;
    }

    public void setSource(NodeId source) {
        this.source = source;
    }

    public RemoteCallIdentifier getRpc() {
        return rpc;
    }

    public void setRpc(RemoteCallIdentifier rpc) {
        this.rpc = rpc;
    }

    public Serializable getMessage() {
        return message;
    }

    public void setMessage(Serializable message) {
        this.message = message;
    }
}
