package OML.parameters

import breeze.linalg.{DenseMatrix => BreezeDenseMatrix, DenseVector => BreezeDenseVector}
import OML.math.{DenseVector, SparseVector, Vector}

/** This class represents a weight matrix with an intercept vector,
  * as it is required for many supervised learning tasks
  *
  * @param A The matrix of weights
  * @param b The intercept (bias) vector weight
  */
case class MatrixLinearModelParameters(var A: BreezeDenseMatrix[Double], var b: BreezeDenseVector[Double])
  extends LearningParameters {

  size = A.cols * A.rows + b.length
  bytes = 8 * size

  def this() = this(BreezeDenseMatrix.zeros(4, 4), BreezeDenseVector.zeros(2))

  override def equals(obj: Any): Boolean = {
    obj match {
      case MatrixLinearModelParameters(w, i) => b == i && A.equals(w)
      case _ => false
    }
  }

  override def toString: String = s"MatrixLinearModelParameters([${A.rows}x${A.cols}], ${A.toDenseVector}, $b)"

  override def +(num: Double): LearningParameters = MatrixLinearModelParameters(A + num, b + num)

  override def +=(num: Double): LearningParameters = {
    A = A + num
    b = b + num
    this
  }

  override def +(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixLinearModelParameters(a, b_) => MatrixLinearModelParameters(A + a, b + b_)
    }
  }

  override def +=(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixLinearModelParameters(a, b_) =>
        A = A + a
        b = b + b_
        this
    }
  }

  override def -(num: Double): LearningParameters = this + (-num)

  override def -=(num: Double): LearningParameters = this += (-num)

  override def -(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixLinearModelParameters(a, b_) => this + MatrixLinearModelParameters(-a, -b_)
    }
  }

  override def -=(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixLinearModelParameters(a, b_) => this += MatrixLinearModelParameters(-a, -b_)
    }
  }

  override def *(num: Double): LearningParameters = MatrixLinearModelParameters(A * num, b * num)

  override def *=(num: Double): LearningParameters = {
    A = A * num
    b = b * num
    this
  }

  override def getCopy(): LearningParameters = this.copy()

  def flatten(): BreezeDenseVector[Double] = BreezeDenseVector.vertcat(A.toDenseVector, b)

  override def toDenseVector(): Vector = DenseVector.denseVectorConverter.convert(flatten())

  override def toSparseVector(): Vector = SparseVector.sparseVectorConverter.convert(flatten())
}

