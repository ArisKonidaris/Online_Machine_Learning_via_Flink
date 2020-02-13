package oml.mlAPI.dataBuffers

import oml.math.Point

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class TrainingSet(override var data_set: ListBuffer[Point], override var max_size: Int) extends DataSet[Point] {

  def this() = this(ListBuffer[Point](), 500000)

  def this(training_set: ListBuffer[Point]) = this(training_set, 500000)

  def this(max_size: Int) = this(ListBuffer[Point](), max_size)

  override var merges: Int = 0

  override def overflowCheck(): Unit = {
    if (data_set.length > max_size)
      data_set.remove(Random.nextInt(max_size + 1))
  }

  override def append(data: Point): Unit = {
    data_set += data
    overflowCheck()
  }

  override def insert(index: Int, data: Point): Unit = {
    data_set.insert(index, data)
    overflowCheck()
  }

  override def length: Int = data_set.length

  override def clear(): Unit = {
    merges = 0
    data_set.clear()
    max_size = 500000
  }

  override def merge(dataSet: DataSet[Point]): TrainingSet = super.merge(dataSet).asInstanceOf[TrainingSet]

}
