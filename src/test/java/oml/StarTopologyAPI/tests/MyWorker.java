package oml.StarTopologyAPI.tests;

import oml.StarTopologyAPI.annotations.InitOp;
import oml.StarTopologyAPI.annotations.MergeOp;
import oml.StarTopologyAPI.annotations.QueryOp;
import oml.StarTopologyAPI.annotations.ProcessOp;
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

    @ProcessOp
    public void process() {

    }

    @InitOp
    public void init() {

    }

    @MergeOp
    public void merge() {

    }

    @QueryOp
    public void query() {

    }

}
