package OML.StarProtocolAPI;


import java.io.Serializable;

public interface Node {

    // This is ParameterServer message
    void receiveMsg(Integer operation, Serializable message);

    // This is mlpipeline.processPoint
    void receiveTuple(Serializable tuple);

    void receiveControlMessage(Serializable request);
}
