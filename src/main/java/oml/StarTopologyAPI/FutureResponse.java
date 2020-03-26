package oml.StarTopologyAPI;

import java.io.Serializable;
import java.util.function.Consumer;

public class FutureResponse<T extends Serializable> implements Response<T>, Consumer<T> {

    Consumer<T> consumer = null;

    @Override
    public void to(Consumer<T> consumer) {
        this.consumer = consumer;
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
}
