package oml.mlAPI.dataBuffers

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class DataSet[T <: java.io.Serializable](var data_buffer: ListBuffer[T], var max_size: Int)
  extends DataBuffer[T] {

  def this() = this(ListBuffer[T](), 500000)

  def this(training_set: ListBuffer[T]) = this(training_set, 500000)

  def this(max_size: Int) = this(ListBuffer[T](), max_size)

  override def overflowCheck(): Option[T] = {
    if (data_buffer.length > max_size)
      Some(data_buffer.remove(Random.nextInt(max_size + 1)))
    else
      None
  }

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
    merges = 0
    data_buffer.clear()
    max_size = 500000
  }

  override def merge(dataSet: DataBuffer[T]): DataSet[T] = super.merge(dataSet).asInstanceOf[DataSet[T]]

  /** Remove and return the oldest data point request the data set */
  override def pop(): Option[T] = remove(0)

  /** Remove and return a data point from the data set */
  override def remove(index: Int): Option[T] = {
    if (data_buffer.length > index) Some(data_buffer.remove(index)) else None
  }

}
