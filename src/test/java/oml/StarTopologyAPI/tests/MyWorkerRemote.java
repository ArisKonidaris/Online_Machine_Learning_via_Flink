package oml.StarTopologyAPI.tests;

import oml.StarTopologyAPI.annotations.RemoteOp;
import oml.StarTopologyAPI.annotations.RemoteProxy;
import oml.StarTopologyAPI.futures.Response;

@RemoteProxy
interface MyWorkerRemote {

    @RemoteOp(1)
    void greeting(String msg);

    @RemoteOp(2)
    Response<String> whoAreYou();
}
