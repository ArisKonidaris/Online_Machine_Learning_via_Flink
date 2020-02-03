package OML.StarProtocolAPI;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

public class NodeClass {

    // The extracted description of the object
    private Class<?> wrappedClass = null;  // The class of the wrapped object
    private Class<?> proxiedInterface = null;  // The remote proxy interface of the object
    private HashMap<Integer, Method> operationTable =null; // Map opid -> method object
    private Class<?> proxyClass=null;  // the proxy class for this node


    static public void check(boolean cond, String format, Object ... args) {
        if(!cond)
            throw new RuntimeException(String.format(format, args));
    }

    /*
        Set `proxyInterface` to a unique proxy interface that is implemented by the
        class.

        A proxy interface is an interface such that either:
        (a) its definition is decorated with @RemoteProxy, or
        (b) its use in the class is decorated with @Remote
     */
    protected void extractProxyInterface() {
        // get remote interface
        AnnotatedType[] ifaces = wrappedClass.getAnnotatedInterfaces();
        Class<?> pxy_ifc=null;

        for(AnnotatedType i : ifaces) {
            assert i.getType() instanceof Class<?>;
            Class<?> icls = (Class) i.getType();

            if(! i.isAnnotationPresent(Remote.class)
                && ! icls.isAnnotationPresent(RemoteProxy.class) ) continue;
            check(pxy_ifc == null, "Multiple remote interfaces on wrapped class %s", wrappedClass);
            pxy_ifc = (Class) i.getType();
        }
        check(pxy_ifc != null, "No remote interfaces on wrapped class %s", wrappedClass);

        // success
        proxiedInterface = pxy_ifc;
    }



    static public List<Class> getInterfaces(Class c) {
        List<Class> interfaces = new ArrayList<>();
        interfaces.addAll(Arrays.asList(c.getInterfaces()));

        while(!c.getSuperclass().equals(Object.class)){
            c = c.getSuperclass();
            interfaces.addAll(Arrays.asList(c.getInterfaces()));
        }

        return interfaces;
    }


    static public boolean isSerializable(Class c) {
        return getInterfaces(c).contains(Serializable.class);
    }


    /*
        Check a remote method of proxyInterface:
        * is annotated with @RemoteOp
        * every parameter must be Serializable
        * return type must be void
     */
    public void checkRemoteMethod(Method m) {

        check(m.getDeclaredAnnotation(RemoteOp.class)!=null,
                "Method %s is not annotated with @RemoteOp",m);

        for(Class pcls : m.getParameterTypes()) {
            assert pcls!=null;
            check(isSerializable(pcls),
                    "Parameter type %s is not Serializable in method %s of remote proxy %s",
                    pcls, m, proxiedInterface);
        }

        check(m.getReturnType()==void.class,
                "Return type is not void in method %s of remote proxy %s",
                m, proxiedInterface);
    }

    /*
        Check the methods of remoteInterface.
        * Each method is given to checkRemoteMethod
        * All @RemoteOp operation ids are unique
     */
    public void checkRemoteMethods() {
        assert proxiedInterface !=null;

        HashMap<Integer, Method> op2method = new HashMap<>();

        for(Method m : proxiedInterface.getMethods()) {
            checkRemoteMethod(m);
            RemoteOp op = m.getDeclaredAnnotation(RemoteOp.class);
            int opid = op.value();
            check(! op2method.containsKey(opid),
                "Methods %s and %s have the same key",
                m, op2method.get(opid));
            try {
                m.setAccessible(true);
            } catch(SecurityException e) {
                throw new RuntimeException(
                        String.format("Interface %s is not accessible (probably not public)", proxiedInterface),
                        e);
            }
            op2method.put(opid, m);
        }
        operationTable = op2method;
    }

    /*
        Create a dynamic proxy class for the proxied interface
     */
    public void createProxyClass() {
        assert proxiedInterface != null;

        proxyClass = Proxy.getProxyClass(getClass().getClassLoader(),
            new Class[] { proxiedInterface });
    }


    public Class<?> getWrappedClass() { return wrappedClass; }

    public Class<?> getProxiedInterface() { return proxiedInterface; }

    public Map<Integer, Method> getOperationTable() { return operationTable; }

    public Class<?> getProxyClass() {  return proxyClass; }

    protected NodeClass(Class  _wclass) {
        wrappedClass = _wclass;
        extractProxyInterface();
        checkRemoteMethods();
        createProxyClass();
    }

    static protected HashMap<Class<?>, NodeClass> instances = new HashMap<>();

    /*
        Caching instances
     */
    synchronized static public NodeClass forClass(Class<?> _wclass) {
        if(instances.containsKey(_wclass))
            return instances.get(_wclass);
        else {
            NodeClass nc = new NodeClass(_wclass);
            instances.put(_wclass, nc);
            return nc;
        }
    }
}
