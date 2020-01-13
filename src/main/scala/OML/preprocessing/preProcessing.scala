package OML.preprocessing

import OML.common.{Parameter, ParameterMap, WithParameters}
import OML.math.Point

import scala.collection.mutable.ListBuffer

/** The basic trait for data pre processing methods.
  * All those methods contain parameters.
  *
  */
trait preProcessing extends Serializable with WithParameters {

  // =============================== Data transformation methods ===================================

  def transform(point: Point): Point

  def transform(dataSet: ListBuffer[Point]): ListBuffer[Point]

  // =================================== Parameter getter ==========================================

  def getParameterMap: ParameterMap = parameters

  // =================================== Parameter setters =========================================

  def setParameters(params: ParameterMap): preProcessing = {
    if (params.map.keySet subsetOf parameters.map.keySet) parameters ++ params
    this
  }

  def setParameter[T](parameter: Parameter[T], value: T): preProcessing = {
    if (parameters.map.contains(parameter)) parameters.add(parameter, value)
    this
  }
}
