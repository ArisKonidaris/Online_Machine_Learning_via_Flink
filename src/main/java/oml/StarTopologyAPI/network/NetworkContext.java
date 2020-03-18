package oml.StarTopologyAPI.network;

import oml.StarTopologyAPI.sites.NetworkDescriptor;

/**
 * An interface for providing the context of the
 * bipartite network to the user.
 */
public interface NetworkContext {

    /** This should be called by the node when it needs to stop processing incoming data. */
    void pauseDataStream();

    /** This should be called by the node when it needs to stop processing incoming messages. */
    void pauseMessageStream();

    /** This should be called by the node when it is ready to process incoming data. */
    void resumeDataStream();

    /** This should be called by the node when it is ready to process incoming messages. */
    void resumeMessageStream();

    /** A method that provides a description of the Bipartite Network. */
    NetworkDescriptor describeNetwork();

}
