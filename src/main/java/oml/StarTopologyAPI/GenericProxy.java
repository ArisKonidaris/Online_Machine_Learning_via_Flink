package oml.StarTopologyAPI;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GenericProxy implements InvocationHandler {

    Network network;
    Integer target;

    public FutureResponse response = null;

    @Override
    public Object invoke(Object o, Method method, Object[] args) {
        int op = method.getAnnotation(RemoteOp.class).value();
        boolean hasResponse = method.getReturnType().equals(Response.class);
        response = null;
        if(hasResponse) response = new FutureResponse();
        network.send(target, op, args);
        return response;
    }

    public GenericProxy(Network network, Integer target) {
        this.network = network;
        this.target = target;
    }

    static public <RmtIf> RmtIf forNode(Class<RmtIf> cls, int nodeId, Network net) {
        GenericProxy proxy = new GenericProxy(net, nodeId);
        return (RmtIf) Proxy.newProxyInstance(GenericProxy.class.getClassLoader(), new Class<?>[]{cls}, proxy);
    }
}
