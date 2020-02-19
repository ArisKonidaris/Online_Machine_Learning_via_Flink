package oml.StarProtocolAPI;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

public class GenericProxy implements InvocationHandler {

    Network network;
    Integer target;

    Consumer consumer = null;

    @Override
    public Object invoke(Object o, Method method, Object[] args) {
        int op = method.getAnnotation(RemoteOp.class).value();
        boolean hasResponse = NodeClass.methodHasResponse(method);
        if(hasResponse) {
            // save the consumer
            assert args!=null && args.length>0;
            assert args[0] instanceof Consumer;
            consumer = (Consumer) args[0];
        }
        network.send(target, op, args);
        return null;
    }

    public GenericProxy(Network _net, Integer _target) {
        network = _net;
        target = _target;
    }

    static public <RmtIf> RmtIf forNode(Class<RmtIf> cls, int nodeId, Network net) {
        GenericProxy proxy = new GenericProxy(net, nodeId);
        return (RmtIf) Proxy.newProxyInstance(GenericProxy.class.getClassLoader(), new Class<?>[]{cls}, proxy);
    }
}
