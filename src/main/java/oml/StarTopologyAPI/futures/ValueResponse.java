package oml.StarTopologyAPI.futures;

import java.io.Serializable;
import java.util.function.Consumer;

public class ValueResponse<T extends Serializable> implements Response<T> {

    T value;

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void to(Consumer<T> consumer) {
        throw new RuntimeException("Illegal operation on ValueResponse");
    }

    @Override
    public void toSync(Consumer<T> consumer) {
        to(consumer);
    }

    public ValueResponse(T value) {
        this.value = value;
    }
}
