package oml.StarTopologyAPI;

import java.io.Serializable;

public interface Queriable extends Serializable {

    void query(long queryId, Serializable[] buffer);

}
