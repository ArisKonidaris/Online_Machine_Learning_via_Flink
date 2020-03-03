package oml.StarProtocolAPI;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

public interface Node extends Serializable {

    void receiveMsg(Integer operation, Serializable message);

    void receiveTuple(Serializable tuple) throws InvocationTargetException, IllegalAccessException;

    void merge(Node node);

}
