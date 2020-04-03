package oml.StarTopologyAPI;

import oml.StarTopologyAPI.annotations.Inject;
import oml.StarTopologyAPI.futures.BroadcastValueResponse;
import oml.StarTopologyAPI.futures.FutureResponse;
import oml.StarTopologyAPI.futures.PromiseResponse;
import oml.StarTopologyAPI.futures.ValueResponse;
import oml.StarTopologyAPI.network.Mergeable;
import oml.StarTopologyAPI.operations.CallType;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.network.Node;
import oml.StarTopologyAPI.sites.NodeId;
import oml.StarTopologyAPI.sites.NodeType;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GenericWrapper implements Node {

    /**
     * The id of the node running in the Bipartite Network.
     */
    protected NodeId nodeId;

    /**
     * The wrapped object node.
     */
    protected NodeInstance node;

    /**
     * The object node's extracted description.
     */
    protected NodeClass nodeClass;

    /**
     * A map of futures.
     */
    protected Map<Long, Map<Integer, FutureResponse<Serializable>>> futures;

    /**
     * A map used internally for message synchronization.
     */
    protected ArrayList<FutureResponse<Serializable>> newFutures;

    /**
     * The number of synchronous futures.
     */
    protected long syncFutures;

    /**
     * The network where this wrapped node object is connected to.
     */
    protected Network network;

    /**
     * Proxies for the disjoint nodes of the Bipartite Graph.
     */
    protected HashMap<Integer, Object> proxyMap;

    /**
     * A proxy that broadcasts to all disjoint nodes of the Bipartite Graph.
     */
    protected Object broadcastProxy;

    /**
     * A proxy to answer queries.
     */
    protected Object querierProxy;

    /**
     * A flag determining if the wrapped node can process data.
     */
    protected boolean processData;

    protected NodeId currentCaller;

    protected RemoteCallIdentifier currentRPC;

    public GenericWrapper(NodeId nodeId, NodeInstance node, Network network) {
        this.nodeId = nodeId;
        this.node = node;
        this.nodeClass = NodeClass.forClass(node.getClass());
        futures = new HashMap<>();
        newFutures = new ArrayList<>();
        syncFutures = 0L;
        this.network = network;
        broadcastProxy = null;
        proxyMap = new HashMap<>();
        processData = true;
        Injections();
        init();

    }

    /**
     * This method Injects proxies for communicating with the remote nodes of the Bipartite Network and for answering
     * queries to a querier.
     */
    private void Injections() {

        // Acquire all the declared fields in the node's class hierarchy, along with the proxy Interface.
        Class<?> current = nodeClass.getWrappedClass();
        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(current.getDeclaredFields()));
        while (!current.getSuperclass().equals(Object.class)) {
            current = current.getSuperclass();
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        // Keep only the injected fields
        fields = fields
                .stream()
                .filter(x -> x.isAnnotationPresent(Inject.class))
                .collect(Collectors.toCollection(ArrayList::new));

        // Check the validity of the injections
        NodeClass.check(!fields.isEmpty(),
                "No remote node proxies on wrapped class %s",
                nodeClass.getWrappedClass());

        NodeClass.check(fields.size() == 2,
                "The Injection annotation is used for injecting network components. You cannot " +
                        " inject additional fields in wrapped class %s.",
                nodeClass.getWrappedClass());

        // Finding the interfaces of the proxies.
        Class<?> proxyInterface = ExtractGenerics.findSubClassParameterType(node, NodeInstance.class, 0);
        assert proxyInterface != null;
        Class<?> querierInterface = ExtractGenerics.findSubClassParameterType(node, NodeInstance.class, 1);
        assert querierInterface != null;

        // Map proxy creation.
        int numberOfSpokes = network.describe().getNumberOfSpokes();
        int numberOfHubs = network.describe().getNumberOfHubs();
        for (int i = 0; i < ((nodeId.isHub()) ? numberOfSpokes : numberOfHubs); i++) {
            NodeId proxyIP = new NodeId((nodeId.isHub()) ? NodeType.SPOKE : NodeType.HUB, i);
            proxyMap.put(i, GenericProxy.forNode(proxyInterface, this, network, proxyIP));
        }

        // Braodcast proxy creation.
        broadcastProxy = GenericProxy.forNode(proxyInterface,
                this,
                network,
                new NodeId((nodeId.isHub()) ? NodeType.SPOKE : NodeType.HUB, Integer.MAX_VALUE)
        );

        // Querier proxy creation.
        querierProxy = GenericProxy.forNode(querierInterface, this, network, null);


        try {
            Field networkContext = fields.get(0);
            networkContext.setAccessible(true);
            networkContext.set(node,
                    NetworkContext.forNode(
                            network.describe().getNetworkId(),
                            network.describe().getNumberOfHubs(),
                            network.describe().getNumberOfSpokes(),
                            nodeId.getNodeId(),
                            querierProxy,
                            proxyMap,
                            broadcastProxy
                    )
            );
            Field wrapperField = fields.get(1);
            wrapperField.setAccessible(true);
            wrapperField.set(node, this);
        } catch (SecurityException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Something went wrong while injecting the NetworkContext in wrapped class %s",
                            nodeClass.getWrappedClass()), e);
        }
        assert !proxyMap.isEmpty();


    }

    @Override
    public void init() {
        if (nonEmpty()) {
            try {
                Method m = nodeClass.getInitMethod();
                m.invoke(node);
                checkNewFutures();
            } catch (Exception e) {
                throw new RuntimeException("Failed wrapper.init", e);
            }
        }
    }

    @Override
    public void receiveQuery(long queryId, Serializable query) {
        if (nonEmpty()) {
            try {
                Method m = nodeClass.getQueryMethod();
                m.invoke(node, queryId, network.describe().getNetworkId(), query);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed wrapper.receiveQuery", e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void receiveMsg(NodeId source, RemoteCallIdentifier rpc, Serializable tuple) {
        if (nonEmpty()) {
            try {
                currentCaller = source;
                currentRPC = rpc;
                if (rpc.getCallType().equals(CallType.RESPONSE)) {
                    if (futures.containsKey(rpc.getCallNumber())) {
                        Map<Integer, FutureResponse<Serializable>> future = futures.get(rpc.getCallNumber());
                        if (future.containsKey(source.getNodeId())) {
                            FutureResponse<Serializable> f = future.get(source.getNodeId());
                            f.accept(tuple);
                            if (f.isSync()) {
                                syncFutures -= 1;
                                if (syncFutures == 0)
                                    unblock();
                            }
                            future.remove(source.getNodeId());
                            if (future.isEmpty()) futures.remove(rpc.getCallNumber());
                        } else System.out.println("No such future from source " + source.getNodeId());
                    } else System.out.println("No futures for the responseCallNumber " + rpc.getCallNumber());
                } else {
                    Method m = nodeClass.getOperationTable().get(rpc.getOperation());
                    Object[] args = (Object[]) tuple;
                    if (rpc.getCallType().equals(CallType.ONE_WAY)) {
                        m.invoke(node, args);
                    } else if (rpc.getCallType().equals(CallType.TWO_WAY)) {
                        Object ret = m.invoke(node, args);
                        assert (ret instanceof ValueResponse ||
                                ret instanceof PromiseResponse ||
                                ret instanceof BroadcastValueResponse);
                        if (ret instanceof ValueResponse) {
                            ValueResponse resp = (ValueResponse) ret;
                            network.send(nodeId,
                                    source,
                                    new RemoteCallIdentifier(CallType.RESPONSE, null, rpc.getCallNumber()),
                                    resp.getValue());
                        } else if (ret instanceof BroadcastValueResponse) {
                            BroadcastValueResponse resp = (BroadcastValueResponse) ret;
                            resp.broadcastResponse();
                        }
                    } else {
                        throw new RuntimeException("Unknown RPC type");
                    }
                }
                checkNewFutures();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed wrapper.receiveMsg", e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void receiveTuple(Serializable tuple) {
        try {
            if (nonEmpty()) {
                Object[] args = (Object[]) tuple;
                nodeClass.getProccessMethod().invoke(node, args);
                checkNewFutures();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed wrapper.receiveTuple", e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void merge(Mergeable[] nodes) {
        assert nodes instanceof GenericWrapper[];
        try {
            if (nonEmpty()) {
                ArrayList<Object> mergeableNodes = new ArrayList<>();
                for (Mergeable node : nodes) mergeableNodes.add(((GenericWrapper) node).getNode());
                nodeClass.getMergeMethod().invoke(this.node, mergeableNodes.toArray());
                if (syncFutures > 0) block();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed wrapper.merge", e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void merge(NodeId nodeId, NodeInstance node, Network network, Mergeable[] nodes) {
        WrapNode(nodeId, node, network);
        merge(nodes);
    }

    public void merge(int index, Mergeable[] nodes) {
        assert index <= nodes.length;
        GenericWrapper wrapper = ((GenericWrapper) nodes[index]);
        WrapNode(wrapper.getNodeId(), wrapper.getNode(), wrapper.getNetwork());
        org.apache.commons.lang.ArrayUtils.remove(nodes, index);
        merge(nodes);
    }

    private void WrapNode(NodeId nodeId, NodeInstance node, Network network) {
        this.nodeId = nodeId;
        this.node = node;
        this.nodeClass = NodeClass.forClass(node.getClass());
        this.network = network;
        broadcastProxy = null;
        proxyMap.clear();
        futures.clear();
        syncFutures = 0L;
        Injections();
    }

    public boolean isEmpty() {
        return node == null;
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    public boolean isBlocked() {
        return !processData;
    }

    protected void block() {
        processData = false;
    }

    protected void unblock() {
        processData = true;
    }

    public NodeInstance getNode() {
        return node;
    }

    public void setNode(NodeInstance node) {
        this.node = node;
        setNodeClass(NodeClass.forClass(node.getClass()));
    }

    public NodeClass getNodeClass() {
        return nodeClass;
    }

    public void setNodeClass(NodeClass nodeClass) {
        this.nodeClass = nodeClass;
    }

    public Network getNetwork() {
        return network;
    }

    public Map<Long, Map<Integer, FutureResponse<Serializable>>> getFutures() {
        return futures;
    }

    public ArrayList<FutureResponse<Serializable>> getNewFutures() {
        return newFutures;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    public Object getBroadcastProxy() {
        return broadcastProxy;
    }

    public HashMap<Integer, Object> getProxyMap() {
        return proxyMap;
    }

    public boolean getProcessData() {
        return processData;
    }

    private void checkNewFutures() {
        if (!newFutures.isEmpty()) {
            for (FutureResponse<Serializable> newFuture : newFutures)
                if (newFuture.isSync())
                    syncFutures += 1;
            if (syncFutures > 0 || !isBlocked())
                block();
            newFutures.clear();
        }
    }

}
