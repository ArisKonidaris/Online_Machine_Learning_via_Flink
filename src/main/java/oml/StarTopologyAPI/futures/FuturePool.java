package oml.StarTopologyAPI.futures;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * A class returned by a proxy of a node when it calls a method on a remote node.
 *
 * @param <T> The type of the Serializable response values.
 */
public class FuturePool<T extends Serializable> implements Response<T>, Consumer<T> {

    Collection<FutureResponse<T>> futureMap;

    public FuturePool(Collection<FutureResponse<T>> futureMap) {
        this.futureMap = futureMap;
    }

    @Override
    public void to(Consumer<T> consumer) {
        if (futureMap != null)
            for (FutureResponse<T> future : futureMap)
                future.to(consumer);
    }

    @Override
    public void toSync(Consumer<T> consumer) {
        if (futureMap != null)
            for (FutureResponse<T> future : futureMap)
                future.toSync(consumer);
    }

    @Override
    public void accept(T t) {
        throw new UnsupportedOperationException("accept() called on FuturePool");
    }

    @Override
    public T getValue() {
        throw new UnsupportedOperationException("getValue() called on FuturePool");
    }
}
