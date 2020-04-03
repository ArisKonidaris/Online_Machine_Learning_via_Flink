package oml.StarTopologyAPI.futures;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A class returned by a proxy of a node when it calls a method on a remote node.
 *
 * @param <T> The type of the Serializable response value.
 */
public class FutureResponse<T extends Serializable> implements Response<T>, Consumer<T> {

    /**
     * A {@link Consumer} to provide the callback to be run when the response arrives.
     */
    protected Consumer<T> consumer = null;

    /**
     * This flag determines if the future is blocking.
     */
    protected boolean sync = false;

    @Override
    public void to(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void toSync(Consumer<T> consumer) {
        to(consumer);
        sync = true;
    }

    @Override
    public T getValue() {
        throw new UnsupportedOperationException("getValue() called on FutureResponse");
    }

    @Override
    public void accept(T value) {
        if (consumer != null)
            consumer.accept(value);
    }

    public boolean isSync() {
        return sync;
    }

}
