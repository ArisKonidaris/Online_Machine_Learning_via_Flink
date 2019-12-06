package OML.preprocessing

import OML.math.Point

abstract class learningPreprocessor extends preprocessing {

  protected var learnable: Boolean = true

  def isLearning: Boolean = learnable

  def freezeLearning(): Unit = learnable = false

  def enableLearning(): Unit = learnable = true

  def fit(vector: Point): Unit
}
