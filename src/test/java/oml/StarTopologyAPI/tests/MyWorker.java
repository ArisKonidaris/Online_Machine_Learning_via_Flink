package oml.StarTopologyAPI.tests;

import oml.StarTopologyAPI.annotations.MergeOp;
import oml.StarTopologyAPI.annotations.QueryOp;
import oml.StarTopologyAPI.annotations.ReceiveTuple;
import oml.StarTopologyAPI.futures.Response;

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

    @QueryOp
    public void query() {

    }

}
