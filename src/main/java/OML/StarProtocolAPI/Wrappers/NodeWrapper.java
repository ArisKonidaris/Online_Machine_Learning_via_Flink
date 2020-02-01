package OML.StarProtocolAPI.Wrappers;

import java.io.Serializable;

public interface NodeWrapper extends Serializable {
    void receive(Serializable tuple);
}
