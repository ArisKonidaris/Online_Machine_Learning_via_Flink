package oml.mlAPI.dataBuffers.removestrategy

import oml.mlAPI.dataBuffers.DataBuffer

import scala.collection.mutable.ListBuffer

trait RemoveStrategy[T <: Serializable] extends Serializable {

  def removeTuple(dataSet: DataBuffer[T]): Option[T]

  def remove(dataSet: DataBuffer[T]): ListBuffer[T] = {
    assert(dataSet.length > dataSet.max_size)
    val extraData = new ListBuffer[T]()
    while (dataSet.length > dataSet.max_size)
      extraData += removeTuple(dataSet).get
    extraData
  }

}
