package OML.StarProtocolAPI.Wrappers;

import java.io.Serializable;

public interface ProcessorWrapper extends NodeWrapper {

    // This is processPoint
    void consumeTuple(Serializable tuple);

    void consumeControlMessage(Serializable tuple);
}
