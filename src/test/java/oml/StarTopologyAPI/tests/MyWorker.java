package oml.StarTopologyAPI.tests;

import oml.StarTopologyAPI.MergeOp;
import oml.StarTopologyAPI.ReceiveTuple;
import oml.StarTopologyAPI.Response;

public class MyWorker implements MyWorkerRemote {

    int greetCounter = 0;

    @Override
    public void greeting(String msg) {
        System.out.println("Hello world " + msg);
        greetCounter++;
    }

    @Override
    public Response<String> whoAreYou() {
        return Response.of("Vasilis");
    }

    @ReceiveTuple
    public void process() {

    }

    @MergeOp
    public void merge() {

    }
}
