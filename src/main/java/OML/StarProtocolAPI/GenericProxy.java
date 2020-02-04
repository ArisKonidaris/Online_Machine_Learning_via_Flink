package OML.StarProtocolAPI;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GenericProxy implements InvocationHandler {

    Network network;
    Integer target;

    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        int op = method.getAnnotation(RemoteOp.class).value();
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
