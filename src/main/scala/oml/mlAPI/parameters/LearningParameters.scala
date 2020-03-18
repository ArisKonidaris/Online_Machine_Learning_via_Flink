package oml.mlAPI.parameters

import oml.math.Vector

/** The base trait representing the learning hyperparameters of a machine learning algorithm.
  *
  */

trait LearningParameters extends Serializable {

  var fitted: Long = 0
  var size: Int = _
  var bytes: Int = _

  def get_fitted: Long = this.fitted
  def get_size: Int = size
  def get_bytes: Int = bytes

  def set_fitted(fitted: Long): Unit = this.fitted = fitted
  def set_size(size: Int): Unit = this.size = size
  def set_bytes(bytes: Int): Unit = this.bytes = bytes

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

  def slice(range: (Int, Int), sparse: Boolean): Vector

  def slice(range: (Int, Int)): Vector = slice(range, sparse = false)

  def sliceRequirements(indexRanges: (Int, Int)): Unit =
    require(indexRanges._1 >= 0 && indexRanges._1 <= indexRanges._2 && indexRanges._2 <= get_size - 1)

}