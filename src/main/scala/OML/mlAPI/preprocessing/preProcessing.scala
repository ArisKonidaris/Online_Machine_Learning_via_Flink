package OML.mlAPI.preprocessing

import OML.math.Point
import OML.mlAPI.WithParams

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

  override def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): preProcessing = this

  override def setParameters(parameterMap: mutable.Map[String, Any]): preProcessing = this

}
