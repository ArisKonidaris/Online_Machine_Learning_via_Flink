package oml.StarTopologyAPI;

import oml.POJOs.Request;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * This is a class that implements a Flink Wrapper.
 * Various distributed synchronization methods are implemented here.
 * The purpose of this class is to connect the Distributed Star Topology
 * API to the Apache Flink.
 */
public class FlinkWrapper extends GenericWrapper {

    private Integer nodeId; // The unique id of a node running in the Star topology

    public FlinkWrapper(Integer node_id, WorkerGenerator generator) {
        super();
        this.nodeId = node_id;
    }

    public FlinkWrapper(Integer node_id, Object node, Network net) {
        super(node, net);
        this.nodeId = node_id;
        InjectProxy(net);
    }

    private void InjectProxy(Network net) {
        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(node.getClass().getDeclaredFields()));
        Class<?> current = node.getClass();
        while (!current.getSuperclass().equals(Object.class)) {
            current = current.getSuperclass();
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        Field hub_proxy = null;
        for (Field f : fields) {
            if (f.isAnnotationPresent(Inject.class)) {
                hub_proxy = f;
                try {
                    hub_proxy.setAccessible(true);
                    hub_proxy.set(node, GenericProxy.forNode(hub_proxy.getType(), nodeId, net));
                } catch (SecurityException | IllegalAccessException e) {
                    throw new RuntimeException(
                            String.format("Field %s is not accessible (probably not public)", hub_proxy),
                            e);
                }
            }
        }

        NodeClass.check(hub_proxy != null,
                "No coordinator proxies on wrapped class %s",
                nodeClass.getWrappedClass());
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

}
