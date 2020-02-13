package oml.StarProtocolAPI;

import java.io.Serializable;

public interface NodeGenerator extends Serializable {
    Node generate(Serializable config);
}
