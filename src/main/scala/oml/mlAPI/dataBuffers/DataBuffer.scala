package oml.mlAPI.dataBuffers

import oml.common.OMLTools.mergeBufferedPoints

import scala.collection.mutable.ListBuffer
import scala.util.Random

trait DataBuffer[T <: Serializable] extends Serializable {

  /** The data set structure. */
  var data_buffer: ListBuffer[T]

  /** The capacity of the data set data structure.
    * This is done to prevent potential overflows. */
  var max_size: Int

  /** The number of times this worker has been merged with other ones */
  var merges: Int

  /** Setters */
  def getDataBuffer: ListBuffer[T] = data_buffer

  def getMaxSize: Int = max_size

  def getMerges: Int = merges

  /** Getters */
  def setDataBuffer(data_set: ListBuffer[T]): Unit = this.data_buffer = data_set

  def setMaxSize(max_size: Int): Unit = this.max_size = max_size

  def setMerges(merges: Int): Unit = this.merges = merges

  /** A method that returns true if the data set is empty */
  def isEmpty: Boolean = data_buffer.isEmpty

  /** A method that returns true if the data set is non empty */
  def nonEmpty: Boolean = data_buffer.nonEmpty

  /** A method that signals the end of the merging procedure of DadaSet objects */
  def completeMerge(): Unit = merges = 0

  /** If the data set becomes too large, the oldest
    * data point is discarded to prevent memory overhead.
    */
  def overflowCheck(): Option[T]

  /** Append a data point to the data set */
  def append(data: T): Option[T]

  /** Insert a data point into the data set */
  def insert(index: Int, data: T): Option[T]

  /** The length of the data set */
  def length: Int = data_buffer.length

  /** Clears the data set */
  def clear(): Unit

  def merge(dataSet: DataBuffer[T]): DataBuffer[T] = {
    merges += 1
    max_size = dataSet.getMaxSize
    if (dataSet.nonEmpty) {
      if (isEmpty) {
        data_buffer = dataSet.getDataBuffer
      } else {
        data_buffer = mergeBufferedPoints(1, length,
          0, dataSet.length,
          data_buffer, dataSet.getDataBuffer,
          merges)
        while (length > max_size)
          data_buffer.remove(Random.nextInt(length))
      }
    }
    this
  }

}
