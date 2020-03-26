package oml.StarTopologyAPI;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

public interface Node extends Mergeable, Serializable {

    void receiveMsg(Integer operation, Serializable message);

    void receiveTuple(Serializable tuple) throws InvocationTargetException, IllegalAccessException;

}
