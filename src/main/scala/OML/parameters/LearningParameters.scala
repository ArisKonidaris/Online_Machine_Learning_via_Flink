package OML.parameters

import OML.math.Vector

/** The base train representing the learning parameters of a machine learning algorithm.
  *
  */

abstract class LearningParameters extends Serializable {

  protected var size: Int = _
  protected var bytes: Int = _

  def set_size(size: Int): Unit = this.size = size

  def set_bytes(bytes: Int): Unit = this.bytes = bytes

  def get_size(): Int = size

  def get_bytes(): Int = bytes

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

  def getCopy(): LearningParameters

  def toDenseVector(): Vector

  def toSparseVector(): Vector

}