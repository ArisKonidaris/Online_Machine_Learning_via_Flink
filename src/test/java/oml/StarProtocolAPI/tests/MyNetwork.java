package oml.StarProtocolAPI.tests;

import oml.StarProtocolAPI.GenericWrapper;
import oml.StarProtocolAPI.Network;
import oml.StarProtocolAPI.Node;

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
    public boolean send(Integer destination, Integer operation, Serializable message) {
        if(operation==-100) {
            response = message;
            return true;
        }

        Node wrapper = wrappers.getOrDefault(destination, null);
        if (wrapper == null) return false;
        wrapper.receiveMsg(operation, message);
        return true;
    }

    @Override
    public boolean broadcast(Integer operation, Serializable message) {
        for (Node wrapper : wrappers.values()) {
            wrapper.receiveMsg(operation, message);
        }
        return true;
    }

    @Override
    public int describe() {
        return 0;
    }
}
