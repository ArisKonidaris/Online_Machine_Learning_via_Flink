package oml.mlAPI.parameters

import oml.math.Vector

/** The base trait representing the learning hyper parameters of a machine learning algorithm.
  *
  */
trait LearningParameters extends Serializable {

  protected var fitted: Long = 0
  protected var size: Int = 0
  protected var bytes: Int = 0

  def getFitted: Long = this.fitted

  def get_size: Int = size

  def get_bytes: Int = bytes

  def getSizes: Array[Int]

  def set_fitted(fitted: Long): Unit = this.fitted = fitted

  def set_size(size: Int): Unit = this.size = size

  def set_bytes(bytes: Int): Unit = this.bytes = bytes

  def equals(obj: Any): Boolean

  def toString: String

  def +(num: Double): LearningParameters

  def +=(num: Double): LearningParameters

  def +(params: LearningParameters): LearningParameters

  def +=(params: LearningParameters): LearningParameters

  def -(num: Double): LearningParameters

  def -=(num: Double): LearningParameters

  def -(params: LearningParameters): LearningParameters

  def -=(params: LearningParameters): LearningParameters

  def *(num: Double): LearningParameters

  def *=(num: Double): LearningParameters

  def /(num: Double): LearningParameters

  def /=(num: Double): LearningParameters

  def getCopy: LearningParameters

  def toDenseVector: Vector

  def toSparseVector: Vector

  def slice(range: Range, sparse: Boolean): Vector

  def slice(range: Range): Vector = slice(range, sparse = false)

  def sliceRequirements(range: Range): Unit = require(range.getEnd <= get_size - 1)

  def genDescriptor: (LearningParameters, Boolean, Range) => ParameterDescriptor

  def generateParams: ParameterDescriptor => LearningParameters

}