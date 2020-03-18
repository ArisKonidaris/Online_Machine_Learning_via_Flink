package oml.mlAPI.dataBuffers

import oml.common.OMLTools.mergeBufferedPoints
import oml.mlAPI.dataBuffers.removestrategy.{RandomRemoveStrategy, RemoveOldestStrategy, RemoveStrategy}

import scala.collection.mutable.ListBuffer

trait DataBuffer[T <: java.io.Serializable] extends java.io.Serializable {

  /** The data set structure. */
  var data_buffer: ListBuffer[T]

  /** The capacity of the data set data structure.
    * This is done to prevent potential overflows. */
  var max_size: Int

  /** The number of times this worker has been merged with other ones */
  var merges: Int = 0

  /** This is the removal strategy of data from the buffer */
  var remove_strategy: RemoveStrategy[T] = RemoveOldestStrategy[T]()

  /** This is the removal strategy of data from the buffer when merging two data buffers */
  var merging_remove_strategy: RemoveStrategy[T] = RandomRemoveStrategy[T]()

  /** Getters */
  def getDataBuffer: ListBuffer[T] = data_buffer

  def getMaxSize: Int = max_size

  def getMerges: Int = merges

  def getRemoveStrategy: RemoveStrategy[T] = remove_strategy

  def getMergingRemoveStrategy: RemoveStrategy[T] = merging_remove_strategy

  /** Setters */
  def setDataBuffer(data_set: ListBuffer[T]): Unit = this.data_buffer = data_set

  def setMaxSize(max_size: Int): Unit = this.max_size = max_size

  def setMerges(merges: Int): Unit = this.merges = merges

  def setRemoveStrategy(remove_strategy: RemoveStrategy[T]): Unit = this.remove_strategy = remove_strategy

  def setMergingRemoveStrategy(merging_remove_strategy: RemoveStrategy[T]): Unit = {
    this.merging_remove_strategy = merging_remove_strategy
  }

  /** A method that returns true if the data set is empty */
  def isEmpty: Boolean = data_buffer.isEmpty

  /** A method that returns true if the data set is non empty */
  def nonEmpty: Boolean = data_buffer.nonEmpty

  /** A method that signals the end of the merging procedure of DadaBuffer objects */
  def completeMerge(): Option[ListBuffer[T]] = {
    merges = 0
    if (length > max_size) Some(merging_remove_strategy.remove(this)) else None
  }

  /** If the data set becomes too large, the oldest
    * data point is discarded to prevent memory overhead.
    */
  def overflowCheck(): Option[T]

  /** Append a data point to the data set */
  def append(data: T): Option[T]

  /** Insert a data point into the data set */
  def insert(index: Int, data: T): Option[T]

  /** Remove and return the oldest data point request the data set */
  def pop(): Option[T]

  /** Remove and return a data point from the data set */
  def remove(index: Int): Option[T]

  /** The length of the data set */
  def length: Int = data_buffer.length

  /** Clears the data set */
  def clear(): Unit

  def merge(dataSet: DataBuffer[T]): DataBuffer[T] = {

    try {
      assert(max_size == dataSet.getMaxSize)
    } catch {
      case _: Throwable =>
        println(s"Merging two Data Buffers with different capacities. " +
          s"${this} has maximum capacity of $max_size and $dataSet has maximum capacity of ${dataSet.max_size}. " +
          s"Setting new maximum buffer capacity to the max max of those two.")
        max_size = Math.max(max_size, dataSet.getMaxSize)
    }

    if (dataSet.nonEmpty) {
      if (isEmpty) {
        data_buffer = dataSet.getDataBuffer
      } else {
        merges += 1
        data_buffer = mergeBufferedPoints(1, length,
          0, dataSet.length,
          data_buffer, dataSet.getDataBuffer,
          merges)
      }
    }

    this
  }

}
