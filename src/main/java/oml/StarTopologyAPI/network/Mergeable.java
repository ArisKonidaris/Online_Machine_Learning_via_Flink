package oml.StarTopologyAPI.network;

import java.io.Serializable;

public interface Mergeable extends Serializable {

    void merge(Mergeable[] mergeables);

}
