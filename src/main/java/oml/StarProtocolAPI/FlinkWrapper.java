package oml.StarProtocolAPI;

import oml.POJOs.Request;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class FlinkWrapper extends GenericWrapper {

    private Integer node_id;
    private WorkerGenerator generator;

    public FlinkWrapper(Integer node_id, WorkerGenerator generator) {
        super();
        this.node_id = node_id;
        this.generator = generator;
    }

    public FlinkWrapper(Integer node_id, WorkerGenerator generator, Request request, Network net) {
        super(generator.generate(request), net);
        this.node_id = node_id;
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
            hub_proxy.set(node, GenericProxy.forNode(hub_proxy.getType(), node_id, net));
        } catch (SecurityException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Field %s is not accessible (probably not public)", hub_proxy),
                    e);
        }
    }

    public void setNode(Request request, Network net) {
        if (generator != null) setNode(generator.generate(request));
        InjectProxy(net);
    }

    public WorkerGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(WorkerGenerator generator) {
        this.generator = generator;
    }

    public int getNode_id() {
        return node_id;
    }

    public void setNode_id(int node_id) {
        this.node_id = node_id;
    }

    public void setNode_id(Integer node_id) {
        this.node_id = node_id;
    }
}
