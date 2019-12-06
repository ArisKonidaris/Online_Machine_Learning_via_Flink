package OML.math
//import breeze.linalg.DenseVector

/** A trait representing a data point required for
  * machine learning tasks.
  *
  */
trait Point extends Serializable {
  var vector: Vector

  def setVector(vector: Vector): Unit = this.vector = vector

  def getVector: Vector = vector

  def toList: List[Double] = vector.toList
}

/** A data point without a label. Could be used for
  * prediction or unsupervised machine learning.
  *
  * @param vector The data point features
  */
case class UnlabeledPoint(var vector: Vector) extends Point {

  def this() = this(DenseVector())

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
case class LabeledPoint(var label: Double, var vector: Vector) extends Point {

  def this() = this(0.0, DenseVector())

  def getLabel: Double = label

  def setLabel(label: Double): Unit = this.label = label

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
