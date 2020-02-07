package OML.StarProtocolAPI;


import java.io.Serializable;

public interface Node {

    // This is ParameterServer message
    void receiveMsg(Integer operation, Serializable message);

    // This is ml pipeline processPoint
    void receiveTuple(Serializable tuple);

    void receiveControlMessage(Serializable request);

    void merge(Node node);

}
