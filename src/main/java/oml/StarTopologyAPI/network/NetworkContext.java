package oml.StarTopologyAPI.network;

import oml.StarTopologyAPI.sites.NetworkDescriptor;

/**
 * An interface for providing the context of the
 * bipartite network to the user.
 */
public interface NetworkContext {

    /** This should be called by the node when it needs to stop processing incoming data. */
    void pauseStream();

    /** This should be called by the node when it is ready to process incoming data. */
    void resumeStream();

    /** A method that provides a description of the Bipartite Network. */
    NetworkDescriptor describeNetwork();

}
