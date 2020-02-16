package oml.StarProtocolAPI;

import java.io.Serializable;

public interface WorkerGenerator extends Serializable {
    Object generate(Serializable config);
}
