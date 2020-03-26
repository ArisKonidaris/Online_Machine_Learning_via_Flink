package oml.mlAPI.dataBuffers.removestrategy

import oml.mlAPI.dataBuffers.DataSet

case class RemoveOldestStrategy[T <: java.io.Serializable]() extends RemoveStrategy[T] {
  override def removeTuple(dataSet: DataSet[T]): Option[T] = dataSet.pop()
}
