package oml.StarTopologyAPI;

import com.fasterxml.uuid.Generators;
import oml.StarTopologyAPI.annotations.RemoteOp;
import oml.StarTopologyAPI.futures.FutureResponse;
import oml.StarTopologyAPI.futures.Response;
import oml.StarTopologyAPI.operations.CallType;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.sites.NodeId;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GenericProxy implements InvocationHandler, Serializable {

    /** The wrapped node object that possesses this proxy. */
    private final GenericWrapper nodeWrapper;

    /** The network used by this proxy to send messages to remote nodes. */
    private final Network network;

    /** The targeted remote node. */
    private final NodeId target;

    /** A counter for identifying the responses of the remote nodes. */
    private long futureCounter;

    /** The ids of the proxied methods. */
    private Map<Method, String> methodIds;

    @Override
    public Object invoke(Object o, Method method, Object[] args) {
        FutureResponse<Serializable> response = null;
        RemoteCallIdentifier rpc = new RemoteCallIdentifier();

        try {
            rpc.setOperation(methodIds.get(method));
            boolean hasResponse = method.getReturnType().equals(Response.class);
            if (hasResponse) {
                response = new FutureResponse<>();
                nodeWrapper.addFuture(futureCounter, response);
                rpc.setCall_type(CallType.TWO_WAY);
                rpc.setCall_number(futureCounter);
                if (futureCounter == Long.MAX_VALUE)
                    futureCounter = 0L;
                else
                    futureCounter++;
            } else {
                rpc.setCall_type(CallType.ONE_WAY);
            }

            if (target == null)
                network.send(nodeWrapper.nodeId, null, rpc, args);
            else if (target.getNodeId() == Integer.MAX_VALUE)
                network.broadcast(nodeWrapper.nodeId, rpc, args);
            else
                network.send(nodeWrapper.nodeId, target, rpc, args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public GenericProxy(Class rmtIf, GenericWrapper node_wrapper, Network network, NodeId target) {
        this.nodeWrapper = node_wrapper;
        this.network = network;
        this.target = target;
        futureCounter = 0L;
        methodIds = new HashMap<>();

        Class<?> current = rmtIf;
        ArrayList<Method> methods = new ArrayList<>(Arrays.asList(rmtIf.getDeclaredMethods()));
        while (!current.getSuperclass().equals(Object.class)) {
            current = current.getSuperclass();
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));
        }
        for (Method method : methods) {
            NodeClass.check(method.getDeclaredAnnotation(RemoteOp.class) != null,
                    "Method %s is not annotated with @RemoteOp", method);
            for (Parameter param : method.getParameters()) {
                Class pcls = param.getType();
                NodeClass.check(NodeClass.isSerializable(pcls),
                        "Parameter type %s is not Serializable request method %s of remote proxy %s",
                        pcls, method, rmtIf);
            }
            NodeClass.check(method.getReturnType() == void.class || method.getReturnType() == Response.class,
                    "Return type is not void request method %s of remote proxy %s",
                    method, rmtIf);
            methodIds.put(method, Generators.nameBasedGenerator()
                    .generate(method.getName() + Arrays.toString(method.getParameterTypes()))
                    .toString()
            );
        }
    }

    static public <RmtIf> RmtIf forNode(Class<RmtIf> cls, GenericWrapper node_wrapper, Network network, NodeId target) {
        GenericProxy proxy = new GenericProxy(cls, node_wrapper, network, target);
        return (RmtIf) Proxy.newProxyInstance(GenericProxy.class.getClassLoader(), new Class<?>[]{cls}, proxy);
    }

    public GenericWrapper getNodeWrapper() {
        return nodeWrapper;
    }

    public Network getNetwork() {
        return network;
    }

    public NodeId getTarget() {
        return target;
    }

    public long getFutureCounter() {
        return futureCounter;
    }

    public Map<Method, String> getMethodIds() {
        return methodIds;
    }

}
