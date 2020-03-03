package oml.message.mtypes

import oml.math.{DenseVector, LabeledPoint, Point}

/** A data point for a worker to train on
  *
  * @param data The received data point
  */
case class DataPoint(var data: Point) extends Serializable {

  def this() = this(LabeledPoint(0, DenseVector(Array[Double](0.0))))

  def getData: Point = data

  def setData(point: Point): Unit = data = point

  override def equals(obj: Any): Boolean = {
    obj match {
      case DataPoint(vec) => data.equals(vec)
      case _ => false
    }
  }

  override def toString: String = s"DataPoint($data)"

}
