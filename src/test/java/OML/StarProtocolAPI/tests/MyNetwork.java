package OML.StarProtocolAPI.tests;

import OML.StarProtocolAPI.GenericWrapper;
import OML.StarProtocolAPI.Network;
import OML.StarProtocolAPI.Node;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MyNetwork implements Network {

    Map<Integer, Node> wrappers;

    public MyNetwork() {
        wrappers = new HashMap<>();
    }

    public Network add(int nodeId, Node wrapper) {
        wrappers.put(nodeId, wrapper);
        return this;
    }

    /* For convenience */
    public <T>  Network addNode(int nodeId, T node) {
        return add(nodeId, new GenericWrapper<T>(node));
    }

    @Override
    public boolean send(Integer destination, Integer operation, Serializable message) {
        Node wrapper = wrappers.getOrDefault(destination, null);
        if(wrapper==null) return false;
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
