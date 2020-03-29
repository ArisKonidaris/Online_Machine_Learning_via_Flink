package oml.StarTopologyAPI.tests;

import oml.StarTopologyAPI.GenericWrapper;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.network.Node;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.sites.NetworkDescriptor;
import oml.StarTopologyAPI.sites.NodeId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MyNetwork implements Network {

    Map<Integer, Node> wrappers;

    Serializable response = null;

    public MyNetwork() {
        wrappers = new HashMap<>();
    }

    public Network add(int nodeId, Node wrapper) {
        wrappers.put(nodeId, wrapper);
        return this;
    }


    public Serializable consumeResponse() {
        Serializable ret = response;
        response = null;
        return ret;
    }

    /* For convenience */
    public Network addNode(int nodeId, Object node) {
        return add(nodeId, new GenericWrapper(node, this));
    }

    @Override
    public void send(NodeId source, NodeId destination, RemoteCallIdentifier rpc, Serializable message) {
        if (operation == -100) {
            response = message;
            return;
        }

        Node wrapper = wrappers.getOrDefault(destination, null);
        if (wrapper == null) return;
        wrapper.receiveMsg(operation, message);
        return;
    }

    @Override
    public void broadcast(NodeId source, RemoteCallIdentifier rpc, Serializable message) {
        for (Node wrapper : wrappers.values()) {
            wrapper.receiveMsg(source, rpc, message);
        }
    }

    @Override
    public NetworkDescriptor describe() {
        return null;
    }
}
