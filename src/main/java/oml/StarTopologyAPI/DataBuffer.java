package oml.StarTopologyAPI;

import scala.Option;

import java.io.Serializable;

public interface DataBuffer<T extends Serializable> extends Mergeable, Serializable {

    /**
     * The capacity of the buffer data structure.
     */
    int getMaxSize();

    /**
     * A method that returns true if the buffer is empty.
     */
    boolean isEmpty();

    /**
     * Returns true if the buffer is non empty.
     */
    default boolean nonEmpty() {
        return !isEmpty();
    }

    /**
     * Append an element to the buffer.
     */
    Option<T> append(T tuple);

    /**
     * Insert an element into the specified position.
     */
    Option<T> insert(int index, T tuple);

    /**
     * Remove the oldest element in the buffer.
     *
     * @return The removed element.
     */
    Option<T> pop();

    /**
     * Remove an element from a specific position.
     *
     * @return The removed element.
     */
    Option<T> remove(int index);

    /**
     * The length of the data buffer.
     */
    int length();

    /**
     * Clears the data buffer.
     */
    void clear();

}
