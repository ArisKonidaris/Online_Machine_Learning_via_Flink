package oml.StarTopologyAPI;

import oml.StarTopologyAPI.annotations.Inject;
import oml.StarTopologyAPI.futures.BroadcastValueResponse;
import oml.StarTopologyAPI.futures.PromiseResponse;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.sites.NodeId;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract class of a node in the Bipartite Network. This class provides immutable information of the Network to the
 * user, like the proxies to the disjoint remote nodes of the Bipartite Network graph, the proxy to an external user
 * that can pose queries to this node, a map of promises made by this node to remote disjoint nodes and more basic
 * information like the id of the network, the id of the node and the number of hubs and spokes.
 *
 * @param <ProxyIfc> The interface for the proxy of the disjoint remote nodes.
 * @param <QueryIfc> The interface of the querier.
 */
public abstract class NodeInstance<ProxyIfc, QueryIfc> {

    @Inject
    private NetworkContext<ProxyIfc, QueryIfc> networkContext;

    @Inject
    private GenericWrapper genericWrapper;

    private boolean broadcasted = false;

    /**
     * A method used by the user when it makes a promise to a disjoint remote node of the Bipartite Network graph. This
     * should be used when the current node does not posses the answer to a question of a disjoint remote node, but will
     * arrive (or produced) at a later time. When the answer is available, {@link #fulfillPromise(long, Serializable)}
     * should be called by the user to send back the answer.
     *
     * @param promiseId The unique id of a promise.
     * @param promise   The promise that was made to the disjoint remote node.
     */
    public <T extends Serializable> void makePromise(long promiseId, PromiseResponse<T> promise) {
        setPromise(promise);
        networkContext.promises.put(promiseId, promise);
    }

    /**
     * The method to call upon the arrival of an answer to a previous request made by a disjoint remote node. This
     * method fulfills the promise given by the current node to a disjoint remote node of the Bipartite Network.
     *
     * @param promiseId The unique id of a promise.
     * @param answer    The answer to the promise.
     */
    public <T extends Serializable> void fulfillPromise(long promiseId, T answer) {
        if (networkContext.promises.containsKey(promiseId)) {
            PromiseResponse promise = networkContext.promises.remove(promiseId);
            promise.sendAnswer(answer);
        }
    }

    /**
     * A method used by the user when it makes a broadcast promise to a disjoint remote node of the Bipartite Network.
     * This should be used when the answer for a specific node is the same for all the disjoint nodes, but do not
     * posses it yet. When the answer is available, {@link #fulfillBroadcastPromise(Serializable)}
     * should be called to broadcast the answer to all the disjoint nodes.
     *
     * @param promise The promise that was made to all the disjoint remote nodse.
     */
    public <T extends Serializable> void makeBroadcastPromise(PromiseResponse<T> promise) {
        if (broadcasted) {
            networkContext.broadcastPromises.clear();
            broadcasted = false;
        }
        setPromise(promise);
        networkContext.broadcastPromises.add(promise);
    }

    /**
     * The method to call upon the arrival of an answer that needs to be broadcasted to all the disjoint nodes of the
     * Bipartite Network.
     *
     * @param answer The answer to the broadcast promise.
     */
    public <T extends Serializable> BroadcastValueResponse<T> fulfillBroadcastPromise(T answer) {
        if (networkContext.broadcastPromises.isEmpty()) {
            throw new RuntimeException("No broadcast promises made to fulfill");
        } else {
            try {
                // Make the private fields accessible.
                Field networkField = networkContext.broadcastPromises.get(0).getClass().getDeclaredField("network");
                networkField.setAccessible(true);
                Field sourceField = networkContext.broadcastPromises.get(0).getClass().getDeclaredField("source");
                sourceField.setAccessible(true);
                Field destField = networkContext.broadcastPromises.get(0).getClass().getDeclaredField("destination");
                destField.setAccessible(true);
                Field rpcField = networkContext.broadcastPromises.get(0).getClass().getDeclaredField("rpc");
                rpcField.setAccessible(true);

                // Get the private fields.
                Network net = (Network) networkField.get(networkContext.broadcastPromises.get(0));
                NodeId src = (NodeId) sourceField.get(networkContext.broadcastPromises.get(0));
                Map<NodeId, RemoteCallIdentifier> rpcs = new HashMap<>();
                for (PromiseResponse promise : networkContext.broadcastPromises) {
                    rpcs.put((NodeId) destField.get(promise), (RemoteCallIdentifier) rpcField.get(promise));
                }

                broadcasted = true;
                return new BroadcastValueResponse<>(net, src, rpcs, answer);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private <T extends Serializable> void setPromise(PromiseResponse<T> promise) {
        try {
            Field networkField = promise.getClass().getDeclaredField("network");
            networkField.setAccessible(true);
            networkField.set(promise, genericWrapper.network);
            Field sourceField = promise.getClass().getDeclaredField("source");
            sourceField.setAccessible(true);
            sourceField.set(promise, genericWrapper.nodeId);
            Field destinationField = promise.getClass().getDeclaredField("destination");
            destinationField.setAccessible(true);
            destinationField.set(promise, genericWrapper.currentCaller);
            Field rpcField = promise.getClass().getDeclaredField("rpc");
            rpcField.setAccessible(true);
            rpcField.set(promise, new RemoteCallIdentifier(genericWrapper.currentRPC.getCallNumber()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // ============================================ Public Getters =====================================================

    public int getNetworkID() {
        return networkContext.networkId;
    }

    public int getNodeId() {
        return networkContext.nodeId;
    }

    public int getNumberOfHubs() {
        return networkContext.numberOfHubs;
    }

    public int getNumberOfSpokes() {
        return networkContext.numberOfSpokes;
    }

    public ProxyIfc getProxy(Integer proxyId) {
        return networkContext.proxies.get(proxyId);
    }

    public ProxyIfc getBroadcastProxy() {
        return networkContext.broadcastProxy;
    }

    public QueryIfc getQuerier() {
        return networkContext.querier;
    }

    public int getCurrentCaller() {
        return genericWrapper.currentCaller.getNodeId();
    }

    public void blockStream() {
        genericWrapper.block();
    }

    public void unblockStream() {
        genericWrapper.unblock();
    }

}
