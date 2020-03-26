package oml.parameters

import breeze.linalg.{DenseMatrix => BreezeDenseMatrix, DenseVector => BreezeDenseVector}
import oml.math.{DenseVector, SparseVector, Vector}

import scala.collection.mutable.ListBuffer

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

  def this() = this(BreezeDenseMatrix.zeros(1, 1), BreezeDenseVector.zeros(1))

  def this(weights: Array[Double]) = this()

  override def getSizes: Array[Int] = Array(A.size, b.size)

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

  override def /(num: Double): LearningParameters = this * (1.0 / num)

  override def /=(num: Double): LearningParameters = this *= (1.0 / num)

  override def getCopy: LearningParameters = this.copy()

  override def toDenseVector: Vector = DenseVector.denseVectorConverter.convert(flatten)

  override def toSparseVector: Vector = SparseVector.sparseVectorConverter.convert(flatten)

  override def flatten: BreezeDenseVector[Double] = BreezeDenseVector.vertcat(A.toDenseVector, b)


  override def generateDescriptor: (LearningParameters, Boolean, Bucket) => ParameterDescriptor = {
    (params: LearningParameters, sparse: Boolean, bucket: Bucket) =>
      new ParameterDescriptor(
        Array(params.asInstanceOf[MatrixModelParameters].A.size, params.asInstanceOf[MatrixModelParameters].b.size),
        params.slice(bucket, sparse),
        bucket,
        params.asInstanceOf[MatrixModelParameters].getFitted
      )
  }

  override def generateParameters(pDesc: ParameterDescriptor): LearningParameters = {
    require(pDesc.getParamSizes.length == 2 && pDesc.getParams.isInstanceOf[DenseVector])

    val weightArrays: ListBuffer[Array[Double]] =
      unwrapData(pDesc.getParamSizes, pDesc.getParams.asInstanceOf[DenseVector].data)
    assert(weightArrays.size == 2)

    MatrixModelParameters(BreezeDenseVector[Double](weightArrays.head).toDenseMatrix,
      BreezeDenseVector[Double](weightArrays.tail.head))
  }

}

