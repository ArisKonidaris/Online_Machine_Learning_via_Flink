package OML.StarProtocolAPI.tests;

import OML.StarProtocolAPI.GenericWrapper;
import OML.StarProtocolAPI.Network;
import OML.StarProtocolAPI.Wrappers.NodeWrapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MyNetwork implements Network {

    Map<Integer, NodeWrapper> wrappers;

    public MyNetwork() {
        wrappers = new HashMap<>();
    }

    public Network add(int nodeId, NodeWrapper wrapper) {
        wrappers.put(nodeId, wrapper);
        return this;
    }

    /* For convenience */
    public <T>  Network addNode(int nodeId, T node) {
        return add(nodeId, new GenericWrapper<T>(node));
    }

    @Override
    public boolean send(Integer destination, Integer operation, Serializable message) {
        NodeWrapper wrapper = wrappers.getOrDefault(destination, null);
        if(wrapper==null) return false;
        wrapper.receive(operation, message);
        return true;
    }

    @Override
    public boolean broadcast(Integer operation, Serializable message) {
        for(NodeWrapper wrapper: wrappers.values()) {
            wrapper.receive(operation, message);
        }
        return true;
    }

    @Override
    public int describe() {
        return 0;
    }
}
