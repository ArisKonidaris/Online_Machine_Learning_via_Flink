package oml.mlAPI.dataBuffers.removestrategy

import oml.mlAPI.dataBuffers.DataSet

import scala.collection.mutable.ListBuffer

trait RemoveStrategy[T <: java.io.Serializable] extends Serializable {

  def removeTuple(dataSet: DataSet[T]): Option[T]

  def remove(dataSet: DataSet[T]): ListBuffer[T] = {
    assert(dataSet.length > dataSet.max_size)
    val extraData = new ListBuffer[T]()
    while (dataSet.length > dataSet.max_size)
      extraData += removeTuple(dataSet).get
    extraData
  }

}
