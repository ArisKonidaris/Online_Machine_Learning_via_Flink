package oml.StarProtocolAPI;

import java.io.Serializable;
import java.util.function.Consumer;

public class ValueResponse<T extends Serializable> implements Response<T> {

    T value;

    @Override
    public T getValue() { return value; }

    @Override
    public void to(Consumer<T> cons) {
        throw new RuntimeException("Illegal operation on ValueResponse");
    }

    public ValueResponse(T _value) {
        value = _value;
    }
}
