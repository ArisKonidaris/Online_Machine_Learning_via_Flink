package oml.StarTopologyAPI;

import oml.StarTopologyAPI.futures.FutureResponse;
import oml.StarTopologyAPI.futures.Response;
import oml.StarTopologyAPI.operations.CallType;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.network.Network;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GenericProxy implements InvocationHandler {

    GenericWrapper nodeWrapper; // The wrapped node object that possesses this proxy
    Network network; // The network used by this proxy to send messages to remote nodes
    Integer target; // The targeted remote node
    Long futureCounter; // A counter for identifying the responses of the remote nodes
    Map<Method, String> methodIds; // The ids of the proxied methods

    @Override
    public Object invoke(Object o, Method method, Object[] args) {
        FutureResponse response = null;
        RemoteCallIdentifier rpc = new RemoteCallIdentifier();

        try {
            rpc.setOperation(methodIds.get(method));
            boolean hasResponse = method.getReturnType().equals(Response.class);
            if (hasResponse) {
                response = new FutureResponse();
                nodeWrapper.getFutures().put(futureCounter, response);
                rpc.setCall_type(CallType.TWO_WAY);
                rpc.setCall_number(futureCounter);
                if (futureCounter == Long.MAX_VALUE)
                    futureCounter = 0L;
                else
                    futureCounter++;
            } else {
                rpc.setCall_type(CallType.ONE_WAY);
            }

            if (target == Integer.MAX_VALUE)
                network.broadcast(nodeWrapper.nodeId, rpc, args);
            else
                network.send(nodeWrapper.nodeId, target, rpc, args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public GenericProxy(Class rmtIf, GenericWrapper node_wrapper, Network network, Integer target) {
        this.nodeWrapper = node_wrapper;
        this.network = network;
        this.target = target;
        futureCounter = 0L;
        methodIds = new HashMap<>();
        for (Method method : rmtIf.getMethods())
            methodIds.put(method, method.getName() + Arrays.toString(method.getParameterTypes()));
    }

    static public <RmtIf> RmtIf forNode(Class<RmtIf> cls, GenericWrapper node_wrapper, Network network, int target) {
        GenericProxy proxy = new GenericProxy(cls, node_wrapper, network, target);
        return (RmtIf) Proxy.newProxyInstance(GenericProxy.class.getClassLoader(), new Class<?>[]{cls}, proxy);
    }
}
