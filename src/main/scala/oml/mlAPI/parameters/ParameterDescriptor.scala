package oml.mlAPI.parameters

import oml.mlAPI.math.{DenseVector, SparseMatrix, SparseVector, Vector}

import scala.collection.mutable

/** A Serializable POJO case class for sending the parameters over the Network.
  * This class contains all the necessary information to reconstruct the parameters
  * on the receiver side.
  *
  */
class ParameterDescriptor() extends java.io.Serializable {

  /** The class type of the parameters that implements the [[LearningParameters]] interface.*/
  var paramClass: String = _

  /** The sizes of each sub parameter inside the LearningParameters instance. */
  var paramSizes: Array[Int] = _

  var bucket: Bucket = _

  /** The number of data used come up with these parameters. */
  var fitted: Long = _

  /** A flag to denote if this ParameterDescriptor instance is mergeable. */
  var mergeable: Boolean = _

  /** A TreeMap with the parameter splits. */
  var parameterTree: mutable.TreeMap[(Int, Int), Vector] = _

  def this(paramClass: String,
           paramSizes: Array[Int],
           params: Vector,
           bucket: Bucket,
           fitted: Long,
           mergeable: Boolean) = {
    this()
    this.paramClass = paramClass
    this.paramSizes = paramSizes
    this.bucket = bucket
    this.parameterTree = mutable.TreeMap(getKey(bucket) -> params)
    this.fitted = fitted
    this.mergeable = mergeable
  }

  def this() = this(LinearModelParameters.getClass.getName,
    Array(2, 1),
    DenseVector(Array(1, 1, 0)).toSparseVector,
    Bucket(0, 2),
    0,
    false)

  // ========================================= Merging =============================================

  def getKey(b: Bucket): (Int, Int) = (b.getStart.toInt, b.getEnd.toInt)

  def makeMergeable(): Unit = {
    parameterTree map {
      case (key:(Int, Int), value: SparseVector) => key -> value.toDenseVector
      case other => other
    }
    setMergeable(true)
  }

  def isMergeable: Boolean = getMergeable

  def equalSizes(sizes: Array[Int]): Boolean = {
    if (paramSizes.length != sizes.length) return false
    for ((mySize, size) <- paramSizes zip sizes)
      if (mySize != size) return false
    true
  }

  /** This method merges [[ParameterDescriptor]] objects to reconstruct a split dense model.
    *
    * @param pDesc A [[ParameterDescriptor]] instance.
    * @return The merged [[ParameterDescriptor]].
    */
  def merge(pDesc: ParameterDescriptor): ParameterDescriptor = {

    // The requirements for merging two ParameterDescriptors that are splits of the same.
    require(
        pDesc.getParameterTree.size == 1 &&
        pDesc.getParameterTree.values.seq.head.size == pDesc.getBucket.getSize &&
        paramClass.equals(pDesc.getParamClass) &&
        equalSizes(pDesc.getParamSizes)
    )

    if (!isMergeable) makeMergeable()

    val newParams: DenseVector = {
      pDesc.getParameterTree.values.seq.head match {
        case vector: DenseVector => vector
        case vector: SparseVector => vector.toDenseVector
        case _ => throw new RuntimeException("Non supported parameter type.")
      }
    }

    parameterTree.put(getKey(pDesc.getBucket), newParams)

    this
  }

  def getParameters: Array[Double] = {
    if (!isMergeable) makeMergeable()
    parameterTree.values.reduce((x1: DenseVector, x2: DenseVector) => x1.data ++ x2.data)
  }

  // =================================== Getters ===================================================

  def getParamClass: String = paramClass

  def getParamSizes: Array[Int] = paramSizes

  def getFitted: Long = fitted

  def getBucket: Bucket = bucket

  def getMergeable: Boolean = mergeable

  def getParameterTree: mutable.TreeMap[(Int, Int), Vector] = parameterTree

  // =================================== Setters ===================================================

  def setParamClass(paramClass: String): Unit = this.paramClass = paramClass

  def setParamSizes(paramSizes: Array[Int]): Unit = this.paramSizes = paramSizes

  def setFitted(fitted: Long): Unit = this.fitted = fitted

  def setBucket(bucket: Bucket): Unit = this.bucket = bucket

  def setMergeable(mergeable: Boolean): Unit = this.mergeable = mergeable

  def setParameterTree(parameterTree: mutable.TreeMap[(Int, Int), Vector]): Unit = this.parameterTree = parameterTree

}

