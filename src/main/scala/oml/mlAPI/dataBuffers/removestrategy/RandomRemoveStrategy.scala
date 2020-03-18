package oml.mlAPI.dataBuffers.removestrategy

import oml.mlAPI.dataBuffers.DataSet

import scala.util.Random

case class RandomRemoveStrategy[T <: Serializable]() extends RemoveStrategy[T] {
  override def removeTuple(dataSet: DataSet[T]): Option[T] = dataSet.remove(Random.nextInt(dataSet.length))
}

