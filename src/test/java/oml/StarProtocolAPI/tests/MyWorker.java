package oml.StarProtocolAPI.tests;

import oml.StarProtocolAPI.Response;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

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
}
