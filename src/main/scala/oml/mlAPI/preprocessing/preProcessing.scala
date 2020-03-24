package oml.mlAPI.preprocessing

import oml.mlAPI.math.Point
import oml.mlAPI.utils.WithParams

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** The basic trait for data pre processing methods.
  * All those methods contain hyperparameters.
  *
  */
trait preProcessing extends Serializable with WithParams {

  // =============================== Data transformation methods ===================================

  def transform(point: Point): Point

  def transform(dataSet: ListBuffer[Point]): ListBuffer[Point]

  override def setHyperParameters(hyperParameterMap: mutable.Map[String, AnyRef]): preProcessing = this

  override def addHyperParameter(key: String, value: AnyRef): preProcessing = this

  override def removeHyperParameter(key: String, value: AnyRef): preProcessing = this

  override def setParameters(parameterMap: mutable.Map[String, AnyRef]): preProcessing = this

  override def addParameter(key: String, value: AnyRef): preProcessing = this

  override def removeParameter(key: String, value: AnyRef): preProcessing = this

}
