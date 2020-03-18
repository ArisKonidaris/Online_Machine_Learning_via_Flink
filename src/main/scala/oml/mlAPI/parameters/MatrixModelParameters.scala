package oml.mlAPI.parameters

import breeze.linalg.{DenseMatrix => BreezeDenseMatrix, DenseVector => BreezeDenseVector}
import oml.math.{DenseVector, SparseVector, Vector}

/** This class represents a weight matrix with an intercept vector,
  * as it is required for many supervised learning tasks
  *
  * @param A The matrix of parameters
  * @param b The intercept (bias) vector weight
  */
case class MatrixModelParameters(var A: BreezeDenseMatrix[Double], var b: BreezeDenseVector[Double])
  extends BreezeParameters {

  // (- 1 + Math.sqrt(1 + 4 * length)) / 2

  size = A.cols * A.rows + b.length
  bytes = 8 * size

  def this() = this(BreezeDenseMatrix.zeros(4, 4), BreezeDenseVector.zeros(2))

  def this(weights: Array[Double]) = this()

  override def equals(obj: Any): Boolean = {
    obj match {
      case MatrixModelParameters(w, i) => b == i && A.equals(w)
      case _ => false
    }
  }

  override def toString: String = s"MatrixModelParameters([${A.rows}x${A.cols}], ${A.toDenseVector}, $b)"

  override def +(num: Double): LearningParameters = MatrixModelParameters(A + num, b + num)

  override def +=(num: Double): LearningParameters = {
    A = A + num
    b = b + num
    this
  }

  override def +(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixModelParameters(a, b_) => MatrixModelParameters(A + a, b + b_)
    }
  }

  override def +=(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixModelParameters(a, b_) =>
        A = A + a
        b = b + b_
        this
    }
  }

  override def -(num: Double): LearningParameters = this + (-num)

  override def -=(num: Double): LearningParameters = this += (-num)

  override def -(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixModelParameters(a, b_) => this + MatrixModelParameters(-a, -b_)
    }
  }

  override def -=(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixModelParameters(a, b_) => this += MatrixModelParameters(-a, -b_)
    }
  }

  override def *(num: Double): LearningParameters = MatrixModelParameters(A * num, b * num)

  override def *=(num: Double): LearningParameters = {
    A = A * num
    b = b * num
    this
  }

  override def getCopy: LearningParameters = this.copy()

  override def toDenseVector: Vector = DenseVector.denseVectorConverter.convert(flatten)

  override def toSparseVector: Vector = SparseVector.sparseVectorConverter.convert(flatten)

  override def /(num: Double): LearningParameters = this * (1.0 / num)

  override def /=(num: Double): LearningParameters = this *= (1.0 / num)

  override def flatten: BreezeDenseVector[Double] = BreezeDenseVector.vertcat(A.toDenseVector, b)

}

