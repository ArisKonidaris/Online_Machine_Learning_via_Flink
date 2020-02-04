package OML.StarProtocolAPI;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericWrapper<NT> implements Node {

    NT node;
    NodeClass nodeClass;

    @Override
    public void receiveMsg(Integer operation, Serializable tuple) {
        Method m = nodeClass.getOperationTable().get(operation);
        Object[] args = (Object[]) tuple;
        try {
            m.invoke(node, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed wrapper.receive", e);
        }
    }

    @Override
    public void receiveTuple(Serializable tuple) {

    }

    @Override
    public void receiveControlMessage(Serializable tuple) {

    }

    public GenericWrapper(NT _node) {
        node = _node;
        nodeClass = NodeClass.forClass(_node.getClass());
    }

}
