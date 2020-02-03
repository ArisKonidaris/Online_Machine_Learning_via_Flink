package OML.StarProtocolAPI.Wrappers;


import java.io.Serializable;

public interface NodeWrapper {

    // This is updatePipeline in flatmap2
    void receive(Integer operation, Serializable tuple);
}
