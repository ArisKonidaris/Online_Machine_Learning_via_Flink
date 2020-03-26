package oml.parameters

import oml.math.Vector

/** The base trait representing the learning hyper parameters of a machine learning algorithm. */
trait LearningParameters extends Serializable {

  var fitted: Long = 0
  var size: Int = _
  var bytes: Int = _

  def getFitted: Long = this.fitted
  def getSize: Int = size
  def getBytes: Int = bytes
  def getSizes: Array[Int]

  def setFitted(fitted: Long): Unit = this.fitted = fitted
  def setSize(size: Int): Unit = this.size = size
  def setBytes(bytes: Int): Unit = this.bytes = bytes

  def equals(obj: Any): Boolean
  def toString: String

  def + (num: Double): LearningParameters
  def +=(num: Double): LearningParameters
  def + (params: LearningParameters): LearningParameters
  def +=(params: LearningParameters): LearningParameters

  def - (num: Double): LearningParameters
  def -=(num: Double): LearningParameters
  def - (params: LearningParameters): LearningParameters
  def -=(params: LearningParameters): LearningParameters

  def * (num: Double): LearningParameters
  def *=(num: Double): LearningParameters

  def /(num: Double): LearningParameters

  def /=(num: Double): LearningParameters

  def getCopy: LearningParameters

  def toDenseVector: Vector

  def toSparseVector: Vector

  def slice(range: Bucket, sparse: Boolean): Vector

  def slice(range: Bucket): Vector = slice(range, sparse = false)

  def sliceRequirements(range: Bucket): Unit = require(range.getEnd <= getSize - 1)

  def generateDescriptor: (LearningParameters, Boolean, Bucket) => ParameterDescriptor

  def generateParameters(pDesc: ParameterDescriptor): LearningParameters

}