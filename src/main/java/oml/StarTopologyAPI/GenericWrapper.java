package oml.StarTopologyAPI;

import oml.StarTopologyAPI.annotations.Inject;
import oml.StarTopologyAPI.futures.FutureResponse;
import oml.StarTopologyAPI.futures.PromiseResponse;
import oml.StarTopologyAPI.futures.ValueResponse;
import oml.StarTopologyAPI.network.Mergeable;
import oml.StarTopologyAPI.network.NetworkContext;
import oml.StarTopologyAPI.operations.CallType;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.network.Node;
import oml.StarTopologyAPI.sites.NetworkDescriptor;
import oml.StarTopologyAPI.sites.NodeId;
import oml.StarTopologyAPI.sites.NodeType;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GenericWrapper implements Node, NetworkContext {

    /**
     * The id of the node running in the Bipartite Network.
     */
    protected NodeId nodeId;

    /**
     * The wrapped object node.
     */
    protected Object node;

    /**
     * The object node's extracted description.
     */
    protected NodeClass nodeClass;

    /**
     * A map of futures.
     */
    protected Map<Long, FutureResponse<Serializable>> futures;

    /**
     * A map of synchronous futures.
     */
    protected long syncFutures;

    /**
     * The network where this wrapped node object is connected to.
     */
    protected Network network;

    /**
     * The network description where this wrapped node is connected to.
     */
    protected NetworkDescriptor graph;

    /**
     * Proxy that broadcasts to all disjoint nodes of the Bipartite Graph.
     */
    protected Object broadcastProxy;

    /**
     * Proxies for the disjoint nodes of the Bipartite Graph.
     */
    protected HashMap<NodeId, Object> proxies;

    /**
     * A flag determining of the wrapped node can process data.
     */
    protected boolean processData;

    public GenericWrapper() {
        nodeId = null;
        node = null;
        nodeClass = null;
        futures = new HashMap<>();
        syncFutures = 0;
        network = null;
        graph = null;
        broadcastProxy = null;
        proxies = new HashMap<>();
        processData = false;
    }

    public GenericWrapper(NodeId nodeId, Object node, Network network) {
        this.nodeId = nodeId;
        this.node = node;
        this.nodeClass = NodeClass.forClass(node.getClass());
        this.futures = new HashMap<>();
        syncFutures = 0L;
        this.network = network;
        this.graph = network.describe();
        broadcastProxy = null;
        proxies = new HashMap<>();
        processData = false;
        InjectProxies();
    }

    /**
     * This method Injects the proxies for communicating with the remote nodes of the Bipartite Network.
     */
    private void InjectProxies() {
        Class<?> proxyInterface = null;

        // Acquire all the declared fields in the node class hierarchy and the proxy Interface
        Class<?> current = nodeClass.getWrappedClass();
        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(current.getDeclaredFields()));
        while (!current.getSuperclass().equals(Object.class)) {
            current = current.getSuperclass();
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            if (current.getSuperclass().getSuperclass().equals(Object.class))
                proxyInterface = (Class<?>) ((ParameterizedType) current.getGenericSuperclass()).getActualTypeArguments()[0];
        }
        assert proxyInterface != null;

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
                "The Injection annotation is used for injecting proxies. A HashMap with" +
                        "proxies for the disjoint set of nodes in the Bipartite " +
                        "Network, and one proxy for broadcasting to all those nodes." +
                        "You cannot inject additional fields in wrapped class %s.",
                nodeClass.getWrappedClass());

        try {
            for (Field field : fields) {
                field.setAccessible(true);
                if (Map.class.isAssignableFrom(field.getType())) {
                    for (int i = 0; i < ((nodeId.isHub()) ? graph.getNumberOfSpokes() : graph.getNumberOfHubs()); i++) {
                        NodeId proxyIP = new NodeId((nodeId.isHub()) ? NodeType.SPOKE : NodeType.HUB, i);
                        proxies.put(proxyIP, GenericProxy.forNode(proxyInterface, this, network, proxyIP));
                    }
                    field.set(node, proxies);
                } else {
                    broadcastProxy = GenericProxy.forNode(proxyInterface,
                            this,
                            network,
                            new NodeId((nodeId.isHub()) ? NodeType.SPOKE : NodeType.HUB, Integer.MAX_VALUE)
                    );
                    field.set(node, broadcastProxy);
                }
            }
        } catch (SecurityException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Something went wrong while injecting proxies in wrapped class %s",
                            nodeClass.getWrappedClass()),
                    e);
        }
        assert !proxies.isEmpty();
        assert broadcastProxy != null;
    }

    @Override
    public void receiveMsg(NodeId source, RemoteCallIdentifier rpc, Serializable tuple) {
        if (nonEmpty()) {
            try {
                if (rpc.getCall_type().equals(CallType.RESPONSE)) {
                    if (futures.containsKey(rpc.getCall_number())) {
                        futures.get(rpc.getCall_number()).accept(tuple);
                        if (futures.get(rpc.getCall_number()).isSync()) syncFutures -= 1;
                        futures.remove(rpc.getCall_number());
                    } else System.out.println("No such future for the response " + rpc.toString());
                } else {
                    Method m = nodeClass.getOperationTable().get(rpc.getOperation());
                    Object[] args = (Object[]) tuple;
                    if (rpc.getCall_type().equals(CallType.ONE_WAY)) {
                        m.invoke(node, args);
                    } else if (rpc.getCall_type().equals(CallType.TWO_WAY)) {
                        Object ret = m.invoke(node, args);
                        assert (ret instanceof ValueResponse || ret instanceof PromiseResponse);
                        if (ret instanceof ValueResponse) {
                            ValueResponse resp = (ValueResponse) ret;
                            network.send(nodeId,
                                    source,
                                    new RemoteCallIdentifier(CallType.RESPONSE, null, rpc.getCall_number()),
                                    resp.getValue());
                        } else {
                            PromiseResponse resp = (PromiseResponse) ret;
                            resp.setNetwork(network);
                            resp.setSource(nodeId);
                            resp.setDestination(source);
                            resp.setRpc(new RemoteCallIdentifier(rpc.getCall_number()));
                        }
                    } else {
                        throw new RuntimeException("Unknown RPC type");
                    }
                }
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
                for (Mergeable node: nodes) mergeableNodes.add(((GenericWrapper) node).getNode());
                nodeClass.getMergeMethod().invoke(this.node, mergeableNodes.toArray());
                if (syncFutures > 0) block();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed wrapper.merge", e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void merge(NodeId nodeId, Object node, Network network, Mergeable[] nodes) {
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

    private void WrapNode(NodeId nodeId, Object node, Network network) {
        this.nodeId = nodeId;
        this.node = node;
        this.nodeClass = NodeClass.forClass(node.getClass());
        this.network = network;
        this.graph = network.describe();
        broadcastProxy = null;
        proxies.clear();
        futures.clear();
        syncFutures = 0L;
        InjectProxies();
    }

    @Override
    public void pauseStream() {
        block();
    }

    @Override
    public void resumeStream() {
        setProcessData(true);
    }

    @Override
    public NetworkDescriptor describeNetwork() {
        return null;
    }

    public void addFuture(Long futureIdentifier, FutureResponse<Serializable> future) {
        if (future.isSync()) syncFutures += 1;
        futures.put(futureIdentifier, future);
    }

    public boolean isEmpty() {
        return node == null;
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    public boolean isBlocked() {
        return !getProcessData();
    }

    public void block() {
        setProcessData(false);
    }

    public void unblock() {
        setProcessData(true);
    }

    public Object getNode() {
        return node;
    }

    public void setNode(Object node) {
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

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Map<Long, FutureResponse<Serializable>> getFutures() {
        return futures;
    }

    public void setFutures(Map<Long, FutureResponse<Serializable>> futures) {
        this.futures = futures;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    public NetworkDescriptor getGraph() {
        return graph;
    }

    public void setGraph(NetworkDescriptor graph) {
        this.graph = graph;
    }

    public Object getBroadcastProxy() {
        return broadcastProxy;
    }

    public void setBroadcastProxy(Object broadcastProxy) {
        this.broadcastProxy = broadcastProxy;
    }

    public HashMap<NodeId, Object> getProxies() {
        return proxies;
    }

    public void setProxies(HashMap<NodeId, Object> proxies) {
        this.proxies = proxies;
    }

    public boolean getProcessData() {
        return processData;
    }

    public void setProcessData(boolean processData) {
        this.processData = processData;
    }

    public long getSyncFutures() {
        return syncFutures;
    }

    public void setSyncFutures(long syncFutures) {
        this.syncFutures = syncFutures;
    }

}
