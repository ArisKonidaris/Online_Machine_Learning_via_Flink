package oml.StarProtocolAPI;

import java.io.Serializable;

public interface Node extends Serializable {

    void receiveMsg(Integer operation, Serializable message);

    void receiveTuple(Serializable tuple);

    void merge(Node node);

}
