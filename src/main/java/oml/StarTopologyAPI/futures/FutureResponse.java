package oml.StarTopologyAPI.futures;

import java.io.Serializable;
import java.util.function.Consumer;

public class FutureResponse<T extends Serializable> implements Response<T>, Consumer<T> {

    Consumer<T> consumer = null;
    boolean sync = false;

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
