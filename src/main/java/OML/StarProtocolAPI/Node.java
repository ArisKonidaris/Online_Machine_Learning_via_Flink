package OML.StarProtocolAPI;


import java.io.Serializable;

public interface Node {

    // This is PS message
    void receiveMsg(Integer operation, Serializable message);

    // This is pipeline.processPoint
    void receiveTuple(Serializable tuple);

    void receiveControlMessage(Serializable request);
}
