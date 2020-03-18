package oml.mlAPI.dataBuffers

import oml.StarTopologyAPI.DataBuffer
import oml.StarTopologyAPI.network.Mergeable
import oml.common.OMLTools.mergeBufferedPoints
import oml.mlAPI.dataBuffers.removestrategy.{RandomRemoveStrategy, RemoveOldestStrategy, RemoveStrategy}

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class DataSet[T <: java.io.Serializable](var data_buffer: ListBuffer[T], var max_size: Int)
  extends DataBuffer[T] {

  def this() = this(ListBuffer[T](), 500000)

  def this(training_set: ListBuffer[T]) = this(training_set, 500000)

  def this(max_size: Int) = this(ListBuffer[T](), max_size)

  /**
    * This is the removal strategy of data from the buffer.
    */
  var remove_strategy: RemoveStrategy[T] = RemoveOldestStrategy[T]()

  /**
    * This is the removal strategy of data from the buffer when merging two data buffers.
    */
  var merging_remove_strategy: RemoveStrategy[T] = RandomRemoveStrategy[T]()

  override def isEmpty: Boolean = data_buffer.isEmpty

  override def append(data: T): Option[T] = {
    data_buffer += data
    overflowCheck()
  }

  override def insert(index: Int, data: T): Option[T] = {
    data_buffer.insert(index, data)
    overflowCheck()
  }

  override def length: Int = data_buffer.length

  override def clear(): Unit = {
    data_buffer.clear()
    max_size = 500000
  }

  override def pop(): Option[T] = remove(0)

  override def remove(index: Int): Option[T] = {
    if (data_buffer.length > index) Some(data_buffer.remove(index)) else None
  }

  override def merge(mergeables: Array[Mergeable]): Unit = {
    require(mergeables.isInstanceOf[Array[DataSet[T]]])
    var merges: Int = 0
    for (buffer: DataSet[T] <- mergeables.asInstanceOf[Array[DataSet[T]]]) {
      if (buffer.nonEmpty) {
        if (isEmpty) {
          data_buffer = buffer.getDataBuffer
        } else {
          merges += 1
          data_buffer = mergeBufferedPoints(1, length,
            0, buffer.length,
            data_buffer, buffer.getDataBuffer,
            merges)
        }
      }
    }
  }

  def overflowCheck(): Option[T] = {
    if (data_buffer.length > max_size)
      Some(data_buffer.remove(Random.nextInt(max_size + 1)))
    else
      None
  }

  /**
    * A method that signals the end of the merging procedure of DadaBuffer objects
    */
  def completeMerge(): Option[ListBuffer[T]] =
    if (length > max_size) Some(merging_remove_strategy.remove(this)) else None

  /////////////////////////////////////////// Getters ////////////////////////////////////////////////

  def getDataBuffer: ListBuffer[T] = data_buffer

  def getMaxSize: Int = max_size

  def getRemoveStrategy: RemoveStrategy[T] = remove_strategy

  def getMergingRemoveStrategy: RemoveStrategy[T] = merging_remove_strategy

  /////////////////////////////////////////// Setters ////////////////////////////////////////////////

  def setDataBuffer(data_set: ListBuffer[T]): Unit = this.data_buffer = data_set

  def setMaxSize(max_size: Int): Unit = this.max_size = max_size

  def setRemoveStrategy(remove_strategy: RemoveStrategy[T]): Unit = this.remove_strategy = remove_strategy

  def setMergingRemoveStrategy(merging_remove_strategy: RemoveStrategy[T]): Unit =
    this.merging_remove_strategy = merging_remove_strategy

}
