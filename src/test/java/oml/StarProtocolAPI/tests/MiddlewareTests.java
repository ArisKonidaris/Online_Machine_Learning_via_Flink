package oml.StarProtocolAPI.tests;


import oml.StarProtocolAPI.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MiddlewareTests {

    @Test
    void testNodeClass() {
        Object wobj = new MyWorker();
        NodeClass w = NodeClass.forClass(wobj.getClass());

        assertEquals(MyWorker.class, w.getWrappedClass());
        assertEquals(MyWorkerRemote.class, w.getProxiedInterface());

        Map<Integer, Method> opTable = w.getOperationTable();
        assertEquals(2, opTable.size());

        assertTrue(opTable.containsKey(1));
        assertTrue(opTable.containsKey(2));
    }

    @Test
    public void testGenericWrapper() {
        MyWorker worker = new MyWorker();
        assertEquals(0, worker.greetCounter);

        // lose type
        Object wobj = worker;

        // wrap it
        Node wrapped = new GenericWrapper(wobj);

        // call method
        wrapped.receiveMsg(1, new Object[]{"vsam"});

        assertEquals(1, worker.greetCounter);
    }


    void processName(String name) {
        System.out.println("response callback called");
        assertEquals("Vasilis", name);
    }

    @Test
    void testGenericProxy() {
        MyNetwork myNet = new MyNetwork();
        MyWorker worker = new MyWorker();
        myNet.addNode(1, worker);

        assertEquals(0, worker.greetCounter);

        // get a proxy for worker, just from the remote interface and the nodeId
        MyWorkerRemote wremote = GenericProxy.forNode(MyWorkerRemote.class, 1, myNet);

        // Call method on the proxy
        wremote.greeting("vsam");

        // check that the method was called on the worker
        assertEquals(1, worker.greetCounter);

        // make a call with a response
        //wremote.whoAreYou(name -> processName(name));
        wremote.whoAreYou(this::processName);


    }
}
