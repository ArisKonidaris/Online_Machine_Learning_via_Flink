package oml.mlAPI.parameters

import oml.mlAPI.math.{DenseVector, SparseVector, Vector}

import breeze.linalg.{DenseMatrix => BreezeDenseMatrix, DenseVector => BreezeDenseVector}
import scala.collection.mutable.ListBuffer

/** This class represents a weight matrix with an intercept (bias) vector.
  *
  * @param A The matrix of parameters.
  * @param b The intercept (bias) vector weight.
  */
case class MatrixBias(var A: BreezeDenseMatrix[Double], var b: BreezeDenseVector[Double])
  extends BreezeParameters {

  size = A.cols * A.rows + b.length
  bytes = 8 * size

  def this() = this(BreezeDenseMatrix.zeros(1, 1), BreezeDenseVector.zeros(1))

  def this(weights: Array[Double]) = this()

  override def getSizes: Array[Int] = Array(A.size, b.size)

  override def equals(obj: Any): Boolean = {
    obj match {
      case MatrixBias(w, i) => b == i && A.equals(w)
      case _ => false
    }
  }

  override def toString: String = s"MatrixBias([${A.rows}x${A.cols}], ${A.toDenseVector}, $b)"

  override def +(num: Double): LearningParameters = MatrixBias(A + num, b + num)

  override def +=(num: Double): LearningParameters = {
    A = A + num
    b = b + num
    this
  }

  override def +(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixBias(a, b_) => MatrixBias(A + a, b + b_)
      case _ => throw new RuntimeException("The provided LearningParameter Object is non-compatible " +
        "for addition with a MatrixBias Object.")
    }
  }

  override def +=(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixBias(a, _b) =>
        A = A + a
        b = b + _b
        this
      case _ => throw new RuntimeException("The provided LearningParameter Object is non-compatible " +
        "for addition with a MatrixBias Object.")
    }
  }

  override def -(num: Double): LearningParameters = this + (-num)

  override def -=(num: Double): LearningParameters = this += (-num)

  override def -(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixBias(a, b_) => this + MatrixBias(-a, -b_)
      case _ => throw new RuntimeException("The provided LearningParameter Object is non-compatible " +
        "for subtraction with a MatrixBias Object.")
    }
  }

  override def -=(params: LearningParameters): LearningParameters = {
    params match {
      case MatrixBias(a, b_) => this += MatrixBias(-a, -b_)
      case _ => throw new RuntimeException("The provided LearningParameter Object is non-compatible " +
        "for subtraction with a MatrixBias Object.")
    }
  }

  override def *(num: Double): LearningParameters = MatrixBias(A * num, b * num)

  override def *=(num: Double): LearningParameters = {
    A = A * num
    b = b * num
    this
  }

  override def /(num: Double): LearningParameters = this * (1.0 / num)

  override def /=(num: Double): LearningParameters = this *= (1.0 / num)

  override def getCopy: LearningParameters = this.copy()

  override def flatten: BreezeDenseVector[Double] = BreezeDenseVector.vertcat(A.toDenseVector, b)

  override def generateSerializedParams: (LearningParameters, Boolean, Bucket) => (Array[Int], Vector) = {
    (params: LearningParameters, sparse: Boolean, bucket: Bucket) =>
      (Array(params.asInstanceOf[MatrixBias].A.size, params.asInstanceOf[MatrixBias].b.size),
        params.slice(bucket, sparse))
  }

  override def generateParameters(pDesc: ParameterDescriptor): LearningParameters = {
    require(pDesc.getParamSizes.length == 2 && pDesc.getParams.isInstanceOf[DenseVector])

    val weightArrays: ListBuffer[Array[Double]] =
      unwrapData(pDesc.getParamSizes, pDesc.getParams.asInstanceOf[DenseVector].data)
    assert(weightArrays.size == 2)

    MatrixBias(BreezeDenseVector[Double](weightArrays.head).toDenseMatrix,
      BreezeDenseVector[Double](weightArrays.tail.head))
  }

}

