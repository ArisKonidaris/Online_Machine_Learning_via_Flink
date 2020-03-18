package oml.StarTopologyAPI;

import oml.StarTopologyAPI.network.Mergeable;

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
    void append(T tuple);

    /**
     * Insert an element into the specified position.
     */
    void insert(int index, T tuple);

    /**
     * Remove the oldest element in the buffer.
     *
     * @return The removed element.
     */
    T pop();

    /**
     * Remove an element from a specific position.
     *
     * @return The removed element.
     */
    T remove(int index);

    /**
     * The length of the data buffer.
     */
    int length();

    /**
     * Clears the data buffer.
     */
    void clear();

}
