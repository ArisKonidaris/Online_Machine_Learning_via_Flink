package OML.parameters

import OML.math.Vector

/** The base trait representing the learning parameters of a machine learning algorithm.
  *
  */

trait LearningParameters extends Serializable {

  var size: Int = _
  var bytes: Int = _

  def get_size: Int = size

  def get_bytes: Int = bytes

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

  def getCopy: LearningParameters

  def toDenseVector: Vector

  def toSparseVector: Vector

}