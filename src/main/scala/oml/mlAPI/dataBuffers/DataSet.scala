package oml.mlAPI.dataBuffers

import oml.math.Point

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class DataSet(override var data_buffer: ListBuffer[Point], override var max_size: Int) extends DataBuffer[Point] {

  def this() = this(ListBuffer[Point](), 500000)

  def this(training_set: ListBuffer[Point]) = this(training_set, 500000)

  def this(max_size: Int) = this(ListBuffer[Point](), max_size)

  override var merges: Int = 0

  override def overflowCheck(): Option[Point] = {
    if (data_buffer.length > max_size)
      Some(data_buffer.remove(Random.nextInt(max_size + 1)))
    else
      None
  }

  override def append(data: Point): Option[Point] = {
    data_buffer += data
    overflowCheck()
  }

  override def insert(index: Int, data: Point): Option[Point] = {
    data_buffer.insert(index, data)
    overflowCheck()
  }

  override def length: Int = data_buffer.length

  override def clear(): Unit = {
    merges = 0
    data_buffer.clear()
    max_size = 500000
  }

  override def merge(dataSet: DataBuffer[Point]): DataSet = super.merge(dataSet).asInstanceOf[DataSet]


}
