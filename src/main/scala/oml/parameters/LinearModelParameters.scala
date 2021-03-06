package oml.parameters

import breeze.linalg.{DenseVector => BreezeDenseVector, SparseVector => BreezeSparseVector}
import oml.math.{DenseVector, SparseVector, Vector}

import scala.collection.mutable.ListBuffer


/** This class represents a weight vector with an intercept, as it is required for many supervised
  * learning tasks
  *
  * @param weights   The vector of parameters
  * @param intercept The intercept (bias) weight
  */
case class LinearModelParameters(var weights: BreezeDenseVector[Double], var intercept: Double)
  extends BreezeParameters {

  size = weights.length + 1
  bytes = getSize * 8

  def this() = this(BreezeDenseVector.zeros(1), 0)

  def this(weights: Array[Double]) = this(
    BreezeDenseVector(weights.slice(0, weights.length - 1)),
    weights(weights.length - 1)
  )

  def this(denseVector: DenseVector) = this(denseVector.data)

  def this(sparseVector: SparseVector) = this(sparseVector.toDenseVector)

  def this(breezeDenseVector: BreezeDenseVector[Double]) =
    this(breezeDenseVector(0 to breezeDenseVector.length - 2),
      breezeDenseVector.valueAt(breezeDenseVector.length - 1)
    )

  def this(breezeSparseVector: BreezeSparseVector[Double]) = this(breezeSparseVector.toDenseVector)

  override def getSizes: Array[Int] = Array(weights.size, 1)

  override def equals(obj: Any): Boolean = {
    obj match {
      case LinearModelParameters(w, i) => intercept == i && weights.equals(w)
      case _ => false
    }
  }

  override def toString: String = s"LinearModelParameters($weights, $intercept)"

  override def +(num: Double): LearningParameters = LinearModelParameters(weights + num, intercept + num)

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

  override def -(num: Double): LearningParameters = this + (-num)

  override def -=(num: Double): LearningParameters = this += (-num)

  override def -(params: LearningParameters): LearningParameters = {
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

  override def /(num: Double): LearningParameters = this * (1.0 / num)

  override def /=(num: Double): LearningParameters = this *= (1.0 / num)

  override def getCopy: LearningParameters = this.copy()

  override def toDenseVector: Vector = DenseVector.denseVectorConverter.convert(flatten)

  override def toSparseVector: Vector = SparseVector.sparseVectorConverter.convert(flatten)

  override def flatten: BreezeDenseVector[Double] =
    BreezeDenseVector.vertcat(weights, BreezeDenseVector.fill(1) {
      intercept
    })

  override def generateSerializedParams: (LearningParameters, Boolean, Bucket) => (Array[Int], Vector, Bucket) = {
    (params: LearningParameters, sparse: Boolean, bucket: Bucket) =>
      (Array(params.asInstanceOf[LinearModelParameters].weights.length, 1), params.slice(bucket, sparse), bucket)
  }

  override def generateParameters(pDesc: ParameterDescriptor): LearningParameters = {
    require(pDesc.getParamSizes.length == 2 && pDesc.getParamSizes.tail.head == 1)
    require(pDesc.getParams.isInstanceOf[DenseVector])

    val weightArrays: ListBuffer[Array[Double]] =
      unwrapData(pDesc.getParamSizes, pDesc.getParams.asInstanceOf[DenseVector].data)
    assert(weightArrays.size == 2)

    LinearModelParameters(BreezeDenseVector[Double](weightArrays.head), weightArrays.tail.head.head)
  }

}