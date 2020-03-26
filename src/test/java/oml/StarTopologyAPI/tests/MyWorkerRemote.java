package oml.StarTopologyAPI.tests;

import oml.StarTopologyAPI.RemoteOp;
import oml.StarTopologyAPI.RemoteProxy;
import oml.StarTopologyAPI.Response;

@RemoteProxy
interface MyWorkerRemote {

    @RemoteOp(1)
    void greeting(String msg);

    @RemoteOp(2)
    Response<String> whoAreYou();
}
