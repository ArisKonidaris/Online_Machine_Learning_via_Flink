package OML.mlAPI.preprocessing

import OML.math.Point
import scala.collection.mutable.ListBuffer

abstract class learningPreprocessor extends preProcessing {

  protected var learnable: Boolean = true

  def init(point: Point): Unit

  def isLearning: Boolean = learnable

  def freezeLearning(): Unit = learnable = false

  def enableLearning(): Unit = learnable = true

  def fit(point: Point): Unit

  def fit(dataSet: ListBuffer[Point]): Unit
}
