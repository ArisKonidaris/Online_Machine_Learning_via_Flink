package OML.parameters

import breeze.linalg.{DenseVector => BreezeDenseVector, DenseMatrix => BreezeDenseMatrix}

/** This class represents a weight matrix with an intercept vector,
  * as it is required for many supervised learning tasks
  *
  * @param A The matrix of weights
  * @param b The intercept (bias) vector weight
  */
case class MatrixLinearModelParameters(var A: BreezeDenseMatrix[Double], var b: BreezeDenseVector[Double])
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

}

