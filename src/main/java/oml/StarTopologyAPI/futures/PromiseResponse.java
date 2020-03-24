package oml.StarTopologyAPI.futures;

import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.sites.NodeId;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A class representing a promised response.
 *
 * @param <T> The type of the Serializable response value.
 */
public class PromiseResponse<T extends Serializable> implements Response<T> {

    /**
     * The network to send the response to.
     */
    private Network network;

    /**
     * The source of the response.
     */
    private NodeId source;

    /**
     * The destination of the response.
     */
    private NodeId destination;

    /**
     * The identification object of the rpc that awaits the response.
     */
    private RemoteCallIdentifier rpc;

    public PromiseResponse() {
        this.network = null;
        this.source = null;
        this.destination = null;
        this.rpc = null;
    }

    public PromiseResponse(Network network, NodeId source, NodeId destination, Long call_number) {
        this.network = network;
        this.source = source;
        this.destination = destination;
        rpc = new RemoteCallIdentifier(call_number);
    }

    @Override
    public void to(Consumer<T> consumer) {
        throw new UnsupportedOperationException("to() called on PromiseResponse");
    }

    @Override
    public void toSync(Consumer<T> consumer) {
        throw new UnsupportedOperationException("toSync() called on PromiseResponse");
    }

    @Override
    public T getValue() {
        throw new UnsupportedOperationException("getValue() called on PromiseResponse");
    }

    /**
     * This method fulfills the promise by sending the provided answer to the remote node caller.
     * @param answer The Serializable answer to send to the remote caller.
     */
    public void sendAnswer(T answer) {
        network.send(source, destination, rpc, answer);
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public NodeId getSource() {
        return source;
    }

    public void setSource(NodeId source) {
        this.source = source;
    }

    public NodeId getDestination() {
        return destination;
    }

    public void setDestination(NodeId destination) {
        this.destination = destination;
    }

    public RemoteCallIdentifier getRpc() {
        return rpc;
    }

    public void setRpc(RemoteCallIdentifier rpc) {
        this.rpc = rpc;
    }
}