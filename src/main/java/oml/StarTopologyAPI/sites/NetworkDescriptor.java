package oml.StarTopologyAPI.sites;

import java.io.Serializable;

/**
 * An class describing the simple Bipartite Network.
 */
public class NetworkDescriptor implements Serializable {

    /** The id of the Bipartite Network. */
    final protected int networkId;

    /** The number of workers of the Bipartite Network. */
    final protected int numberOfSpokes;

    /** The number of hubs of the Bipartite Network. */
    final protected int numberOfHubs;

    public NetworkDescriptor(int networkId,
                             int numberOfWorkers,
                             int numberOfHubs) {
        this.networkId = networkId;
        this.numberOfSpokes = numberOfWorkers;
        this.numberOfHubs = numberOfHubs;
    }

    public int getNumberOfSpokes() {
        return numberOfSpokes;
    }

    public int getNumberOfHubs() {
        return numberOfHubs;
    }

    public int getNetworkId() {
        return networkId;
    }
}
