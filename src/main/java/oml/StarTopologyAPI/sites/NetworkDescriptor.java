package oml.StarTopologyAPI.sites;


/**
 * An class describing the simple Bipartite Network.
 */
public class NetworkDescriptor {

    /**
     * The id of the Bipartite Network.
     */
    private final int networkId;

    /**
     * The number of workers of the Bipartite Network.
     */
    private final int numberOfSpokes;

    /**
     * The number of hubs of the Bipartite Network.
     */
    private final int numberOfHubs;

    public NetworkDescriptor(int networkId,
                             int numberOfSpokes,
                             int numberOfHubs) {
        this.networkId = networkId;
        this.numberOfSpokes = numberOfSpokes;
        this.numberOfHubs = numberOfHubs;
    }

    public int getNetworkId() {
        return networkId;
    }

    public int getNumberOfSpokes() {
        return numberOfSpokes;
    }

    public int getNumberOfHubs() {
        return numberOfHubs;
    }

}
