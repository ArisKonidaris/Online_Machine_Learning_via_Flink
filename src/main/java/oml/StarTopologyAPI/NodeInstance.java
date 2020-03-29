package oml.StarTopologyAPI;

import oml.StarTopologyAPI.annotations.Inject;
import oml.StarTopologyAPI.futures.PromiseResponse;

import java.io.Serializable;

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

}
