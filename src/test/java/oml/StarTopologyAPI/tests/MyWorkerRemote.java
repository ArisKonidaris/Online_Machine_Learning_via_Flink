package oml.StarTopologyAPI.tests;

import oml.StarTopologyAPI.annotations.RemoteOp;
import oml.StarTopologyAPI.annotations.RemoteProxy;
import oml.StarTopologyAPI.futures.Response;

@RemoteProxy
interface MyWorkerRemote {

    @RemoteOp
    void greeting(String msg);

    @RemoteOp
    Response<String> whoAreYou();
}
