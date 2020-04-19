package oml.mlAPI.dataBuffers.removestrategy

import oml.mlAPI.dataBuffers.DataSet

case class RemoveOldestStrategy[T]() extends RemoveStrategy[T] {
  override def removeTuple(dataSet: DataSet[T]): Option[T] = dataSet.pop
}
