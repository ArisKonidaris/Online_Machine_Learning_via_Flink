package OML.common

import OML.math.Vector
//import breeze.linalg.DenseVector

/** A trait representing a data point required for
  * machine learning tasks.
  *
  */
trait Point extends Serializable {
  val vector: Vector
}

/** A data point without a label. Could be used for
  * prediction or unsupervised machine learning.
  *
  * @param vector The data point features
  */
case class UnlabeledPoint(vector: Vector) extends Point {
  override def equals(obj: Any): Boolean = {
    obj match {
      case uPoint: UnlabeledPoint =>
        vector.equals(uPoint.vector)
      case _ => false
    }
  }

  override def toString: String = s"UnlabeledPoint($vector)"

  def convertToLabeledPoint(label: Double): LabeledPoint = LabeledPoint(label, vector)

}

/** This class represents a vector with an associated label as it is
  * required for many supervised learning tasks.
  *
  * @param label  Label of the data point
  * @param vector The data point features
  */
case class LabeledPoint(label: Double, vector: Vector) extends Point {

  override def equals(obj: Any): Boolean = {
    obj match {
      case labeledPoint: LabeledPoint =>
        vector.equals(labeledPoint.vector) && label.equals(labeledPoint.label)
      case _ => false
    }
  }

  override def toString: String = {
    s"LabeledPoint($label, $vector)"
  }

  def convertToUnlabeledPoint(): UnlabeledPoint = UnlabeledPoint(vector)

}
