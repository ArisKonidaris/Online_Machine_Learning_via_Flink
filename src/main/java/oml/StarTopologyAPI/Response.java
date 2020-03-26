package oml.StarTopologyAPI;


import java.io.Serializable;
import java.util.function.Consumer;

public interface Response<T extends Serializable> {

    void to(Consumer<T> consumer);

    T getValue();

    static <R extends Serializable> Response<R> of(R value) {
        return new ValueResponse<>(value);
    }
}
