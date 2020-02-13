package oml.StarProtocolAPI.tests;

import oml.StarProtocolAPI.RemoteOp;
import oml.StarProtocolAPI.RemoteProxy;

@RemoteProxy
interface MyWorkerRemote {

    @RemoteOp(1)
    void greeting(String msg);
}
