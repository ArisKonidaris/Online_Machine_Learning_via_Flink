package OML.StarProtocolAPI.Wrappers;


import java.io.Serializable;

public interface NodeWrapper {
    void receive(Integer operation, Serializable tuple);
}
