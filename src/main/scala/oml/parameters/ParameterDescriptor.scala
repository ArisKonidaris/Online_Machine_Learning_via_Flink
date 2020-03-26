package oml.parameters

import oml.math.{DenseVector, Vector}

/** A Serializable POJO case class for sending the parameters over the Network.
  * This class contains all the necessary information to reconstruct the parameters
  * on the receiver side.
  *
  */
class ParameterDescriptor(var paramSizes: Array[Int],
                          var params: Vector,
                          var bucket: Bucket,
                          var fitted: Long)
  extends java.io.Serializable {

  def this() = this(Array(1), DenseVector(Array(0.0)), new Bucket(), 0)

  // ========================================= Merging =============================================
//
//  //  /** A TreeMap with the parameter splits. */
//  //  var parameterTree: mutable.TreeMap[(Int, Int), Vector] = _
//  //  this.parameterTree = mutable.TreeMap(getKey(bucket) -> params)
//
//  def getKey(b: Bucket): (Int, Int) = (b.getStart.toInt, b.getEnd.toInt)
//
//  def makeMergeable(): Unit = {
//    parameterTree map {
//      case (key:(Int, Int), value: SparseVector) => key -> value.toDenseVector
//      case other => other
//    }
//    setMergeable(true)
//  }
//
//  def equalSizes(sizes: Array[Int]): Boolean = {
//    if (paramSizes.length != sizes.length) return false
//    for ((mySize, size) <- paramSizes zip sizes)
//      if (mySize != size) return false
//    true
//  }
//
//  /** This method merges [[ParameterDescriptor]] objects to reconstruct a split dense model.
//    *
//    * @param pDesc A [[ParameterDescriptor]] instance.
//    * @return The merged [[ParameterDescriptor]].
//    */
//  def merge(pDesc: ParameterDescriptor): ParameterDescriptor = {
//
//    // The requirements for merging two ParameterDescriptors that are splits of the same.
//    require(
//      pDesc.getParameterTree.size == 1 &&
//        pDesc.getParameterTree.values.seq.head.size == pDesc.getBucket.getSize &&
//        paramClass.equals(pDesc.getParamClass) &&
//        equalSizes(pDesc.getParamSizes)
//    )
//
//    if (!isMergeable) makeMergeable()
//
//    val newParams: DenseVector = {
//      pDesc.getParameterTree.values.seq.head match {
//        case vector: DenseVector => vector
//        case vector: SparseVector => vector.toDenseVector
//        case _ => throw new RuntimeException("Non supported parameter type.")
//      }
//    }
//
//    parameterTree.put(getKey(pDesc.getBucket), newParams)
//
//    this
//  }
//
//  def getParameters: Array[Double] = {
//    if (!isMergeable) makeMergeable()
//    parameterTree
//      .values
//      .fold(Array[Double]())(
//      (accum, vector) => accum.asInstanceOf[Array[Double]] ++ vector.asInstanceOf[DenseVector].data)
//      .asInstanceOf[Array[Double]]
//  }

  // =================================== Getters ===================================================

  def getParamSizes: Array[Int] = paramSizes

  def getParams: Vector = params

  def getFitted: Long = fitted

  def getBucket: Bucket = bucket

  // =================================== Setters ===================================================

  def setParamSizes(paramSizes: Array[Int]): Unit = this.paramSizes = paramSizes

  def setParams(params: Vector): Unit = this.params = params

  def setFitted(fitted: Long): Unit = this.fitted = fitted

  def setBucket(bucket: Bucket): Unit = this.bucket = bucket

}


