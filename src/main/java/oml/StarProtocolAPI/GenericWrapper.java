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
    }

    public GenericWrapper(Object _node, Network network) {
        node = _node;
        nodeClass = NodeClass.forClass(_node.getClass());
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
        if (nonEmpty()) invokeMethodByName("receiveTuple", tuple);
    }

    @Override
    public void merge(Node node) {
        if (nonEmpty()) invokeMethodByName("merge", node);
    }

    public void invokeMethodByName(String method_name, Serializable tuple) {
        try {
            Method[] methods = nodeClass.getWrappedClass().getMethods();
            Method m = null;
            for (Method meth : methods) {
                if (meth.getName().equals(method_name)) {
                    m = meth;
                    break;
                }
            }
            assert m != null;
            Object[] args = (Object[]) tuple;
            m.invoke(node, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed wrapper.%s", method_name), e);
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
}
