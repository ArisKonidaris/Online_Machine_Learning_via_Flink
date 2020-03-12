package oml.StarProtocolAPI;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericWrapper implements Node {

    protected Object node;
    protected NodeClass nodeClass;
    protected Network network;

    public GenericWrapper() {
        node = null;
        nodeClass = null;
        network = null;
    }

    public GenericWrapper(Object node, Network network) {
        this.node = node;
        nodeClass = NodeClass.forClass(node.getClass());
        this.network = network;
    }

    @Override
    public void receiveMsg(Integer operation, Serializable tuple) {
        if (nonEmpty()) {
            Method m = nodeClass.getOperationTable().get(operation);
            Object[] args = (Object[]) tuple;
            try {
                Object ret = m.invoke(node, args);
                if(ret != null) {
                    assert ret instanceof ValueResponse;
                    ValueResponse resp = (ValueResponse) ret;
                    network.send(0,-100, resp.getValue());
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
}
