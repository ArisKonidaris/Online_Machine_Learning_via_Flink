package OML.StarProtocolAPI;

import java.io.Serializable;

public interface Network extends Serializable {
    boolean send(Integer destination, Integer operation, Serializable message);

    boolean broadcast(Integer operation, Serializable message);

    int describe();
}
