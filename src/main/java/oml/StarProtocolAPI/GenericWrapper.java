package oml.StarProtocolAPI;

import com.sun.istack.NotNull;
import oml.message.workerMessage;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericWrapper implements Node {

    Object node;
    NodeClass nodeClass;

    public GenericWrapper() {
        node = null;
        nodeClass = null;
    }

    public GenericWrapper(@NotNull Object _node) {
        node = _node;
        nodeClass = NodeClass.forClass(_node.getClass());
        // TODO: Injections (i.e. proxy)
    }

    @Override
    public void receiveMsg(Integer operation, Serializable tuple) {
        if (!isEmpty()) {
            Method m = nodeClass.getOperationTable().get(operation);
            Object[] args = (Object[]) tuple;
            try {
                m.invoke(node, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed wrapper.receiveMsg", e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void receiveTuple(Serializable tuple) {
        if (!isEmpty()) invokeMethodByName("receiveTuple", tuple);
    }

    @Override
    public void merge(Node node) {
        if (!isEmpty()) invokeMethodByName("merge", node);
    }

    @Override
    public void send(Collector<workerMessage> out) {
        try {
            Method[] methods = nodeClass.getWrappedClass().getMethods();
            Method m = null;
            for (Method meth : methods) {
                if (meth.getName().equals("send")) {
                    m = meth;
                    break;
                }
            }
            assert m != null;
            m.invoke(node, out);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed wrapper.send", e);
        }
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
