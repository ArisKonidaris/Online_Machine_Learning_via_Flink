package OML.message

import OML.math.{DenseVector, LabeledPoint, Point}

/** A data point for a worker to train on
  *
  * @param partition Index of the worker/partition
  * @param data      The received data point
  */
case class DataPoint(var partition: Int, var data: Point) extends Serializable {

  def this() = this(0, LabeledPoint(0, DenseVector(Array[Double](0.0))))

  def getPartition: Int = partition

  def setPartition(partition: Int): Unit = this.partition = partition

  def getData: Point = data

  def setData(point: Point): Unit = data = point

  override def equals(obj: Any): Boolean = {
    obj match {
      case DataPoint(part, lvec) => partition == part && data.equals(lvec)
      case _ => false
    }
  }

  override def toString: String = s"DataPoint($partition, $data)"

}
