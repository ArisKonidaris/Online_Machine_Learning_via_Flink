package oml.StarTopologyAPI;

import oml.StarTopologyAPI.futures.FutureResponse;
import oml.StarTopologyAPI.futures.ValueResponse;
import oml.StarTopologyAPI.operations.CallType;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.network.Node;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class GenericWrapper implements Node {

    protected Integer nodeId; // The unique id of a node running in the Star Topology
    protected Object node; // The wrapped object node
    protected NodeClass nodeClass; // The object node's description
    protected Map<Long, FutureResponse<Serializable>> futures; // A map of futures
    protected Network network; // The network this node object is connected to

    public GenericWrapper() {
        nodeId = null;
        node = null;
        nodeClass = null;
        futures = new HashMap<>();
        network = null;
    }

    public GenericWrapper(Integer nodeId, Object node, Network network) {
        this.nodeId = nodeId;
        this.node = node;
        nodeClass = NodeClass.forClass(node.getClass());
        futures = new HashMap<>();
        this.network = network;
    }

    @Override
    public void receiveMsg(Integer source, RemoteCallIdentifier rpc, Serializable tuple) {
        if (nonEmpty()) {
            try {
                if (rpc.getCall_type().equals(CallType.RESPONSE)) {
                    futures.get(rpc.getCall_number()).accept(tuple);
                    futures.remove(rpc.getCall_number());
                } else {
                    Method m = nodeClass.getOperationTable().get(rpc.getOperation());
                    Object[] args = (Object[]) tuple;
                    if (rpc.getCall_type().equals(CallType.ONE_WAY)) {
                        m.invoke(node, args);
                    } else if (rpc.getCall_type().equals(CallType.TWO_WAY)) {
                        Object ret = m.invoke(node, args);
                        assert ret instanceof ValueResponse;
                        ValueResponse resp = (ValueResponse) ret;
                        network.send(nodeId,
                                source,
                                new RemoteCallIdentifier(CallType.RESPONSE, null, rpc.getCall_number()),
                                resp.getValue());
                    } else {
                        throw new RuntimeException("Unknown RPC type");
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed wrapper.receiveMsg", e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void receiveTuple(Serializable tuple) {
        try {
            if (nonEmpty()) {
                Object[] args = (Object[]) tuple;
                nodeClass.getProccessMethod().invoke(node, args);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed wrapper.receiveTuple", e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void merge(Node node) {
        try {
            if (nonEmpty())
                nodeClass.getMergeMethod().invoke(node, node);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed wrapper.receiveTuple", e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    public boolean isEmpty() {
        return node == null;
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    public Object getNode() {
        return node;
    }

    public void setNode(Object node) {
        this.node = node;
        setNodeClass(NodeClass.forClass(node.getClass()));
    }

    public NodeClass getNodeClass() {
        return nodeClass;
    }

    public void setNodeClass(NodeClass nodeClass) {
        this.nodeClass = nodeClass;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Map<Long, FutureResponse<Serializable>> getFutures() {
        return futures;
    }

    public void setFutures(Map<Long, FutureResponse<Serializable>> futures) {
        this.futures = futures;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }
}
