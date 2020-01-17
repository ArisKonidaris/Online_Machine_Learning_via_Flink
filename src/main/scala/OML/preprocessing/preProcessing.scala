package OML.preprocessing

import OML.common.{Parameter, ParameterMap, WithParameters}
import OML.math.Point

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** The basic trait for data pre processing methods.
  * All those methods contain hyperparameters.
  *
  */
trait preProcessing extends Serializable {

  // =============================== Data transformation methods ===================================

  def transform(point: Point): Point

  def transform(dataSet: ListBuffer[Point]): ListBuffer[Point]

  def setParameters(parameterMap: mutable.Map[String, Any]): preProcessing = this

  def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): preProcessing = this

}
