package OML.StarProtocolAPI.tests;

import OML.StarProtocolAPI.RemoteOp;
import OML.StarProtocolAPI.RemoteProxy;

@RemoteProxy
interface MyWorkerRemote {

    @RemoteOp(1)
    void greeting(String msg);
}
