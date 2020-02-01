package OML.StarProtocolAPI.Wrappers;

import java.io.Serializable;

public interface CoordinatorWrapper extends NodeWrapper {
    void consumeControlMessage(Serializable tuple);
}
