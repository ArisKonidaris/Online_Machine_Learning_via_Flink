package OML.parameters

import breeze.linalg.{DenseVector => BreezeDenseVector}
import OML.math.{Vector, DenseVector, SparseVector}


/** This class represents a weight vector with an intercept, as it is required for many supervised
  * learning tasks
  *
  * @param weights The vector of weights
  * @param intercept The intercept (bias) weight
  */
case class LinearModelParameters(var weights: BreezeDenseVector[Double], var intercept: Double)
  extends LearningParameters {

  size = weights.length + 1
  bytes = 8 * size

  def this() = this(BreezeDenseVector.zeros(1), 0)

  override def equals(obj: Any): Boolean = {
    obj match {
      case LinearModelParameters(w, i) =>  intercept == i && weights.equals(w)
      case _ => false
    }
  }

  override def toString: String = {
    s"LinearModelParameters($weights, $intercept)"
  }

  override def + (num: Double): LearningParameters = LinearModelParameters(weights + num, intercept + num)

  override def +=(num: Double): LearningParameters = {
    weights = weights + num
    intercept += num
    this
  }

  override def +(params: LearningParameters): LearningParameters = {
    params match {
      case LinearModelParameters(w, i) => LinearModelParameters(weights + w, intercept + i)
    }
  }

  override def +=(params: LearningParameters): LearningParameters = {
    params match {
      case LinearModelParameters(w, i) =>
        weights = weights + w
        intercept += i
        this
    }
  }

  override def - (num: Double): LearningParameters = this + (-num)

  override def -=(num: Double): LearningParameters = this += (-num)

  override def - (params: LearningParameters): LearningParameters = {
    params match {
      case LinearModelParameters(w, i) => this + LinearModelParameters(-w, -i)
    }
  }

  override def -=(params: LearningParameters): LearningParameters = {
    params match {
      case LinearModelParameters(w, i) => this += LinearModelParameters(-w, -i)
    }
  }

  override def *(num: Double): LearningParameters = LinearModelParameters(weights * num, intercept * num)

  override def *=(num: Double): LearningParameters = {
    weights = weights * num
    intercept *= num
    this
  }

  override def getCopy(): LearningParameters = this.copy()

  def flatten(): BreezeDenseVector[Double] = BreezeDenseVector.vertcat(weights, BreezeDenseVector.fill(1) {
    intercept
  })

  override def toDenseVector(): Vector = DenseVector.denseVectorConverter.convert(flatten())

  override def toSparseVector(): Vector = SparseVector.sparseVectorConverter.convert(flatten())

}