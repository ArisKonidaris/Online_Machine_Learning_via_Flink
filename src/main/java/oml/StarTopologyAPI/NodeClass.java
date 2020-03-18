package oml.StarTopologyAPI;

import com.fasterxml.uuid.Generators;
import oml.StarTopologyAPI.annotations.*;
import oml.StarTopologyAPI.futures.Response;
import org.apache.commons.lang.ClassUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * A Serializable class representing the extracted description of an object
 */
public class NodeClass implements Serializable {

    private Class<?> wrappedClass; // The class of the wrapped object
    private Class<?> proxiedInterface; // The remote proxy interface of the object
    private HashMap<String, Method> operationTable; // Map opid -> method descriptor object
    private Method processOperation; // The method used to process data
    private Method mergeOperation; // The method used to merge two wrappedClasses
    private Class<?> proxyClass; // the proxy class for this node

    public NodeClass() {
        wrappedClass = null;
        proxiedInterface = null;
        operationTable = null;
        processOperation = null;
        mergeOperation = null;
        proxyClass = null;
    }

    public NodeClass(Class wrappedClass) {
        setUpNodeClass(wrappedClass);
    }

    /*
        Set `proxyInterface` to a unique proxy interface that is implemented by the
        class.

        A proxy interface is an interface such that either:
        (a) its definition is decorated with @RemoteProxy, or
        (b) its use request the class is decorated with @Remote
     */
    protected void extractProxyInterface() {
        // get remote interface
        AnnotatedType[] ifaces = wrappedClass.getAnnotatedInterfaces();
        Class<?> pxy_ifc = null;

        for (AnnotatedType i : ifaces) {
            assert i.getType() instanceof Class<?>;
            Class<?> icls = (Class) i.getType();

            if (!i.isAnnotationPresent(Remote.class) && !icls.isAnnotationPresent(RemoteProxy.class))
                continue;
            check(pxy_ifc == null, "Multiple remote interfaces on wrapped class %s", wrappedClass);
            pxy_ifc = (Class) i.getType();
        }
        check(pxy_ifc != null, "No remote interfaces on wrapped class %s", wrappedClass);

        // success
        proxiedInterface = pxy_ifc;
    }


    static public ArrayList<Object> getInterfaces(Class c) {
        ArrayList<Object> interfaces = new ArrayList<Object>(ClassUtils.getAllInterfaces(c));

        try {
            while (!c.getSuperclass().equals(Object.class)) {
                c = c.getSuperclass();
                interfaces.addAll(Arrays.asList(c.getInterfaces()));
            }
        } catch (NullPointerException ignored) {
        }

        return interfaces;
    }


    static public boolean isSerializable(Class c) {
        return getInterfaces(c).contains(Serializable.class);
    }


    /*
        Check a remote method of proxyInterface:
        * is annotated with @RemoteOp
        * every non-@Response parameter must be Serializable
        * if the first parameter is annotated with @Response then it must be
          of type java.util.function.Consumer
        * return type must be void
     */
    public void checkRemoteMethod(Method m) {

        check(m.getDeclaredAnnotation(RemoteOp.class) != null,
                "Method %s is not annotated with @RemoteOp", m);

        Parameter[] params = m.getParameters();

        for (Parameter param : params) {
            Class pcls = param.getType();

            check(isSerializable(pcls),
                    "Parameter type %s is not Serializable request method %s of remote proxy %s",
                    pcls, m, proxiedInterface);
        }

        check(m.getReturnType() == void.class || m.getReturnType() == Response.class,
                "Return type is not void request method %s of remote proxy %s",
                m, proxiedInterface);
    }

    /*
        Check the methods of remoteInterface.
        * Each method is given to checkRemoteMethod
        * All @RemoteOp operation ids are unique
     */
    public void checkRemoteMethods() {
        assert proxiedInterface != null;

        HashMap<String, Method> op2method = new HashMap<>();

        for (Method m : proxiedInterface.getMethods()) {

            // check that the method is well-formed
            checkRemoteMethod(m);

            // generated a Universal Unique Identifier for the method/operation
            String method_identifier = Generators
                    .nameBasedGenerator()
                    .generate(m.getName() + Arrays.toString(m.getParameterTypes()))
                    .toString();

            // make method object accessible
            try {
                m.setAccessible(true);
            } catch (SecurityException e) {
                throw new RuntimeException(
                        String.format("Interface %s is not accessible (probably not public)", proxiedInterface),
                        e);
            }

            // add to operation table
            op2method.put(method_identifier, m);
        }
        operationTable = op2method;
    }

    public Method checkAuxiliaryMethod(Class<? extends Annotation> C) {
        Class cls = wrappedClass;
        ArrayList<Method> methods = new ArrayList<>(Arrays.asList(cls.getMethods()));
        Method process_method = null;
        for (Method meth : methods) {
            if (meth.isAnnotationPresent(C)) {
                check(process_method == null,
                        "Multiple process methods declared on wrapped class %s",
                        wrappedClass);
                process_method = meth;
            }
        }
        check(process_method != null, "No process tuple method on wrapped class %s", wrappedClass);
        return process_method;
    }

    public void setUpNodeClass(Class<?> wrappedClass) {
        this.wrappedClass = wrappedClass;
        extractProxyInterface();
        checkRemoteMethods();
        processOperation = checkAuxiliaryMethod(ReceiveTuple.class);
        mergeOperation = checkAuxiliaryMethod(MergeOp.class);
        createProxyClass();
    }

    /*
        Create a dynamic proxy class for the proxied interface
     */
    public void createProxyClass() {
        assert proxiedInterface != null;

        proxyClass = Proxy.getProxyClass(getClass().getClassLoader(),
                proxiedInterface);
    }

    static public void check(boolean cond, String format, Object... args) {
        if (!cond)
            throw new RuntimeException(String.format(format, args));
    }

    static protected HashMap<Class<?>, NodeClass> instances = new HashMap<>();

    /*
        Caching instances
     */
    synchronized static public NodeClass forClass(Class<?> _wclass) {
        if (instances.containsKey(_wclass))
            return instances.get(_wclass);
        else {
            NodeClass nc = new NodeClass(_wclass);
            instances.put(_wclass, nc);
            return nc;
        }
    }

    public Class<?> getWrappedClass() {
        return wrappedClass;
    }

    public Class<?> getProxiedInterface() {
        return proxiedInterface;
    }

    public Map<String, Method> getOperationTable() {
        return operationTable;
    }

    public Method getProccessMethod() {
        return processOperation;
    }

    public Method getMergeMethod() {
        return mergeOperation;
    }

    public Class<?> getProxyClass() {
        return proxyClass;
    }

    public void setWrappedClass(Class<?> wrappedClass) {
        setUpNodeClass(wrappedClass);
    }

    public void setProxiedInterface(Class<?> proxiedInterface) {
        this.proxiedInterface = proxiedInterface;
    }

    public void setOperationTable(HashMap<String, Method> operationTable) {
        this.operationTable = operationTable;
    }

    public Method getProcessOperation() {
        return processOperation;
    }

    public void setProcessOperation(Method processOperation) {
        this.processOperation = processOperation;
    }

    public Method getMergeOperation() {
        return mergeOperation;
    }

    public void setMergeOperation(Method mergeOperation) {
        this.mergeOperation = mergeOperation;
    }

    public void setProxyClass(Class<?> proxyClass) {
        this.proxyClass = proxyClass;
    }

    public static HashMap<Class<?>, NodeClass> getInstances() {
        return instances;
    }

    public static void setInstances(HashMap<Class<?>, NodeClass> instances) {
        NodeClass.instances = instances;
    }

}
