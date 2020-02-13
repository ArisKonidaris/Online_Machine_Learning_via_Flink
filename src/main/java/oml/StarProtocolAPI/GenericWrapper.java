package oml.StarProtocolAPI;

import oml.message.workerMessage;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericWrapper implements Node {

    Object node;
    NodeClass nodeClass;

    @Override
    public void receiveMsg(Integer operation, Serializable tuple) {
        Method m = nodeClass.getOperationTable().get(operation);
        try {
            m.invoke(node, tuple);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed wrapper.receiveMsg", e);
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void receiveTuple(Serializable tuple) {
        invokeMethodByName("receiveTuple", tuple);
    }

    @Override
    public void merge(Node node) {
        invokeMethodByName("merge", node);
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
            m.invoke(node, tuple);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed wrapper.%s", method_name), e);
        }
    }

    public GenericWrapper(Object _node) {
        node = _node;
        nodeClass = NodeClass.forClass(_node.getClass());
    }

}
