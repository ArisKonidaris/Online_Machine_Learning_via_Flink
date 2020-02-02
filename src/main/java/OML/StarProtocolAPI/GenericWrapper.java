package OML.StarProtocolAPI;

import OML.StarProtocolAPI.NodeClass;
import OML.StarProtocolAPI.Wrappers.NodeWrapper;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericWrapper<NT> implements NodeWrapper {

    NT node;
    NodeClass nodeClass;

    @Override
    public void receive(Integer operation, Serializable tuple) {
        Method m = nodeClass.getOperationTable().get(operation);
        Object[] args = (Object[])tuple;
        try {
            m.invoke(node, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed wrapper.receive", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed wrapper.receive", e);
        }
    }

    public GenericWrapper(NT _node) {
        node = _node;
        nodeClass = NodeClass.forClass(_node.getClass());
    }

}
