package OML.StarProtocolAPI.tests;

import OML.StarProtocolAPI.Remote;
import OML.StarProtocolAPI.RemoteProxy;

public class MyWorker implements MyWorkerRemote {

    int greetCounter = 0;

    @Override
    public void greeting(String msg) {
        System.out.println("Hello world "+msg);
        greetCounter ++;
    }
}
