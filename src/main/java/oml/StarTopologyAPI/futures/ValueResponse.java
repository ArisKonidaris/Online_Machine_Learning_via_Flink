package oml.StarTopologyAPI.futures;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * This class represents the response to a remote procedure call.
 *
 * @param <T> The type of the Serializable response value.
 */
public class ValueResponse<T extends Serializable> implements Response<T> {

    /**
     * The actual Serializable response value.
     */
    protected T value;

    public ValueResponse(T value) {
        this.value = value;
    }

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

}
