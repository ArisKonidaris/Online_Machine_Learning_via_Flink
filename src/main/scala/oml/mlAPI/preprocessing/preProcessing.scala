package oml.mlAPI.preprocessing

import oml.math.Point
import oml.mlAPI.WithParams

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

  def generatePOJOPreprocessor: oml.POJOs.Preprocessor

  override def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): preProcessing = this

  override def addHyperParameter(key: String, value: AnyRef): preProcessing = this

  override def removeHyperParameter(key: String, value: AnyRef): preProcessing = this

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): preProcessing = this

  override def addParameter(key: String, value: AnyRef): preProcessing = this

  override def removeParameter(key: String, value: AnyRef): preProcessing = this

}
