package oml.StarTopologyAPI;

import oml.StarTopologyAPI.annotations.Inject;
import oml.StarTopologyAPI.futures.PromiseResponse;

import java.io.Serializable;
import java.util.Map;

public abstract class NodeObject<T> implements Serializable {

    /**
     * Proxies for the disjoint nodes of the Bipartite Graph.
     */
    @Inject
    protected Map<Integer, T> proxies;

    /**
     * A proxy that broadcasts to all disjoint nodes of the Bipartite Graph.
     */
    @Inject
    protected T broadcastProxy;

    /**
     * A map of promises that this node has made to the disjoint nodes of the Bipartite Graph.
     */
    private Map<Long, PromiseResponse<Serializable>> promises;

    void fulfillPromise(long promiseId, Serializable answer) {
        if (promises.containsKey(promiseId)) {
            PromiseResponse<Serializable> promise = promises.remove(promiseId);
            promise.sendAnswer(answer);
        }
    }

    public int numberOfProxies() {
        return proxies.size();
    }



}
