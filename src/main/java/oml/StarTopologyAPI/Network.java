package oml.StarTopologyAPI;

import java.io.Serializable;

public interface Network extends Serializable {

    void send(Integer destination, Integer operation, Serializable message);

    void broadcast(Integer operation, Serializable message);

    void describe();
}
