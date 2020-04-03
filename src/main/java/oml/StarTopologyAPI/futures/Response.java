package oml.StarTopologyAPI.futures;


import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A basic interface representing response messages of remote procedure calls.
 * With this interface we enable the Middleware to support future capabilities.
 *
 * @param <T> The type of the Serializable response value.
 */
public interface Response<T extends Serializable> {

    /**
     * Binding a callback to be executed asynchronously.
     */
    void to(Consumer<T> consumer);

    /**
     * Binding a callback to be executed synchronously.
     */
    void toSync(Consumer<T> consumer);

    /**
     * Returns the value of the response.
     *
     * @return <T> A Serializable value.
     */
    T getValue();

    /**
     * A method for creating a ValuedResponse to be send back to the callee.
     * This method is called by the remote node with the returned value of its invoked procedure.
     *
     * @param <R> A Serializable response value.
     */
    static <R extends Serializable> Response<R> respond(R value) {
        return new ValueResponse<>(value);
    }

}
