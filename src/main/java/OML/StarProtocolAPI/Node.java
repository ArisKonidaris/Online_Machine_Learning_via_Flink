package OML.StarProtocolAPI;


import java.io.Serializable;

public interface Node {

    // This is updatePipeline in flatmap2
    void receiveMsg(Integer operation, Serializable tuple);

    // This is pipeline.processPoint
    void receiveTuple(Serializable tuple);

    void receiveControlMessage(Serializable tuple);
}
