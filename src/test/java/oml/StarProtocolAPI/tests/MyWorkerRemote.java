package oml.StarProtocolAPI.tests;

import oml.StarProtocolAPI.RemoteOp;
import oml.StarProtocolAPI.RemoteProxy;
import oml.StarProtocolAPI.Response;

@RemoteProxy
interface MyWorkerRemote {

    @RemoteOp(1)
    void greeting(String msg);

    @RemoteOp(2)
    Response<String> whoAreYou();
}
