package OML.parameters

/** The base train representing the learning parameters of a machine learning algorithm.
  *
  */

trait LearningParameters extends Serializable {
  def equals(obj: Any): Boolean
  def toString: String
  def length: Int
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
}