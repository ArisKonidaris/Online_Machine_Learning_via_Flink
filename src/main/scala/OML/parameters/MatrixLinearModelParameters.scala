package OML.parameters

import breeze.linalg.{DenseVector => BreezeDenseVector, DenseMatrix => BreezeDenseMatrix}

/** This class represents a weight matrix with an intercept vector,
  * as it is required for many supervised learning tasks
  *
  * @param A The matrix of weights
  * @param b The intercept (bias) vector weight
  */
case class MatrixLinearModelParameters(A: BreezeDenseMatrix[Double], var b: BreezeDenseVector[Double])
  extends LearningParameters {

  override def equals(obj: Any): Boolean = {
    obj match {
      case MatrixLinearModelParameters(w, i) => b == i && A.equals(w)
      case _ => false
    }
  }

  override def toString: String = s"MatrixLinearModelParameters([${A.rows}x${A.cols}], ${A.toDenseVector}, $b)"

  override def length: Int = A.cols * A.rows + b.length

  override def +(num: Double): LearningParameters = MatrixLinearModelParameters(A + num, b + num)

  override def +(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixLinearModelParameters(w, i) => MatrixLinearModelParameters(A + w, b + i)
    }
  }

  override def *(num: Double): LearningParameters = MatrixLinearModelParameters(A * num, b * num)

  override def -(num: Double): LearningParameters = this + (-num)

  override def -(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixLinearModelParameters(w, i) => this + MatrixLinearModelParameters(-w, -i)
    }
  }

  def getCopy: LearningParameters = {
    MatrixLinearModelParameters(A, b)
  }

}

