package OML.message

import OML.common.Point

/** A data point for a worker to train on
  *
  * @param part Index of the worker/partition
  * @param data The received data point
  */
case class DataPoint(part: Int, data: Point) extends LearningMessage {

  var partition: Int = part

  override def equals(obj: Any): Boolean = {
    obj match {
      case DataPoint(part, lvec) => partition == part && data.equals(lvec)
      case _ => false
    }
  }

  override def toString: String = s"DataPoint($partition, $data)"

}
