package oml.FlinkBackend.wrappers;

import oml.FlinkBackend.POJOs.Request;
import oml.StarTopologyAPI.GenericProxy;
import oml.StarTopologyAPI.GenericWrapper;
import oml.StarTopologyAPI.NodeClass;
import oml.StarTopologyAPI.WorkerGenerator;
import oml.StarTopologyAPI.annotations.Inject;
import oml.StarTopologyAPI.network.Network;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * This is a class that implements a Flink Wrapper.
 * Various distributed synchronization methods are implemented here.
 * The purpose of this class is to connect the Distributed Star Topology
 * API to Apache Flink.
 */
public class FlinkWrapper extends GenericWrapper {

    // The unique id of a specific network. This is the network that this wrapped node object belongs to
    private Integer NetworkId;

    private WorkerGenerator generator; // A factory class that produces worker nodes

    public FlinkWrapper(WorkerGenerator generator) {
        super();
        this.generator = generator;
    }

    public FlinkWrapper(Integer nodeId, WorkerGenerator generator, Request request, Network net) {
        super(nodeId, generator.generate(request), net);
        this.generator = generator;
        InjectProxy(net);
    }

    private void InjectProxy(Network net) {
        Field hub_proxy = null;

        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(node.getClass().getDeclaredFields()));
        Class<?> current = node.getClass();
        while (!current.getSuperclass().equals(Object.class)) {
            current = current.getSuperclass();
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        for (Field f : fields) {
            if (f.isAnnotationPresent(Inject.class)) {
                NodeClass.check(hub_proxy == null,
                        "Multiple coordinator proxies on wrapped class %s",
                        nodeClass.getWrappedClass());
                hub_proxy = f;
            }
        }
        NodeClass.check(hub_proxy != null,
                "No coordinator proxies on wrapped class %s",
                nodeClass.getWrappedClass());

        try {
            hub_proxy.setAccessible(true);
            hub_proxy.set(node, GenericProxy.forNode(hub_proxy.getType(), this, net, nodeId));
        } catch (SecurityException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Field %s is not accessible (probably not public)", hub_proxy),
                    e);
        }
    }

    public void setNode(Request request, Network network) {
        if (generator != null) setNode(generator.generate(request));
        this.network = network;
        InjectProxy(network);
    }

    public WorkerGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(WorkerGenerator generator) {
        this.generator = generator;
    }

}
