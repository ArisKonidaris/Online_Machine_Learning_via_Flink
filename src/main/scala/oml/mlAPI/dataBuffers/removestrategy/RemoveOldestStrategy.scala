package oml.mlAPI.dataBuffers.removestrategy

import oml.mlAPI.dataBuffers.DataBuffer

case class RemoveOldestStrategy[T <: Serializable]() extends RemoveStrategy[T] {
  override def removeTuple(dataSet: DataBuffer[T]): Option[T] = dataSet.pop()
}
