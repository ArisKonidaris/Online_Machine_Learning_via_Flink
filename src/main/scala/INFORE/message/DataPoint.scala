package INFORE.message

import INFORE.common.Point

/** A data point for a worker to train on
  *
  * @param partition Index of the worker/partition
  * @param data The received data point
  */
case class DataPoint(override val partition: Int, data: Point) extends LearningMessage {

  override def equals(obj: Any): Boolean = {
    obj match {
      case DataPoint(part, lvec) => partition == part && data.equals(lvec)
      case _ => false
    }
  }

  override def toString: String = s"DataPoint($partition, $data)"

}
