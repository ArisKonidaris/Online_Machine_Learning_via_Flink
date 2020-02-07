package OML.StarProtocolAPI.tests;

public class MyWorker implements MyWorkerRemote {

    int greetCounter = 0;

    @Override
    public void greeting(String msg) {
        System.out.println("Hello world " + msg);
        greetCounter++;
    }
}
